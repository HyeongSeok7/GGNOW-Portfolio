document.addEventListener("DOMContentLoaded", () => {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const currentUsername = document.querySelector('meta[name="currentUsername"]')?.getAttribute("content") || "";
    const festivalId = document.querySelector('meta[name="festivalId"]')?.getAttribute("content");

    const reviewForm = document.getElementById("reviewForm");
    const reviewContent = document.getElementById("reviewContent");
    const reviewList = document.getElementById("reviewList");
    const favoriteToggleBtn = document.getElementById("favoriteToggleBtn");
    const reviewLoginHint = document.getElementById("reviewLoginHint");

    const loginPromptModal = document.getElementById("loginPromptModal");
    const loginPromptClose = document.getElementById("loginPromptClose");
    const loginPromptCancel = document.getElementById("loginPromptCancel");
    const loginPromptTitle = document.getElementById("loginPromptTitle");
    const loginPromptMessage = document.getElementById("loginPromptMessage");

    const isLoggedIn = currentUsername.trim().length > 0;

    if (!festivalId) {
        console.error("festivalId를 찾을 수 없습니다.");
        return;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function formatDate(value) {
        if (!value) return "";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;

        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, "0");
        const dd = String(date.getDate()).padStart(2, "0");
        const hh = String(date.getHours()).padStart(2, "0");
        const mi = String(date.getMinutes()).padStart(2, "0");

        return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
    }

    function openLoginPrompt(title, message) {
        if (!loginPromptModal) {
            alert(message || "로그인이 필요합니다.");
            location.href = "/login";
            return;
        }

        loginPromptTitle.textContent = title || "로그인이 필요해요";
        loginPromptMessage.textContent = message || "이 기능은 로그인 후 이용할 수 있어요.";
        loginPromptModal.hidden = false;
        document.body.classList.add("modal-open");
    }

    function closeLoginPrompt() {
        if (!loginPromptModal) return;
        loginPromptModal.hidden = true;
        document.body.classList.remove("modal-open");
    }

    function updateFavoriteButton(isFavorite) {
        if (!favoriteToggleBtn) return;

        favoriteToggleBtn.dataset.favorite = isFavorite ? "true" : "false";
        favoriteToggleBtn.textContent = isFavorite ? "즐겨찾기 해제" : "즐겨찾기 추가";
        favoriteToggleBtn.classList.toggle("is-active", isFavorite);
    }

    function applyGuestView() {
        if (favoriteToggleBtn) {
            favoriteToggleBtn.dataset.favorite = "false";
            favoriteToggleBtn.classList.add("is-guest");
            favoriteToggleBtn.textContent = "즐겨찾기 추가";
        }

        if (reviewForm) {
            reviewForm.classList.add("is-guest");
        }

        if (reviewContent) {
            reviewContent.placeholder = "로그인 후 리뷰를 작성할 수 있어요.";
        }

        if (reviewLoginHint) {
            reviewLoginHint.hidden = false;
        }
    }

    async function loadFavoriteState() {
        if (!favoriteToggleBtn || !isLoggedIn) return;

        try {
            const response = await fetch("/getFavoriteEvents", {
                credentials: "same-origin"
            });

            if (!response.ok) {
                updateFavoriteButton(false);
                return;
            }

            const favoriteEventIds = await response.json();
            const eventId = String(favoriteToggleBtn.dataset.eventId);
            const isFavorite = favoriteEventIds.includes(eventId);

            updateFavoriteButton(isFavorite);
        } catch (error) {
            console.error("즐겨찾기 상태 조회 실패:", error);
            updateFavoriteButton(false);
        }
    }

    async function toggleFavorite() {
        if (!favoriteToggleBtn) return;

        if (!isLoggedIn) {
            openLoginPrompt(
                "로그인이 필요해요",
                "즐겨찾기는 로그인 후 이용할 수 있어요."
            );
            return;
        }

        const eventId = String(favoriteToggleBtn.dataset.eventId);
        const isFavorite = favoriteToggleBtn.dataset.favorite === "true";
        const url = isFavorite ? "/removeFavoriteEvent" : "/addFavoriteEvent";
        const method = isFavorite ? "DELETE" : "POST";

        try {
            const response = await fetch(url, {
                method,
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    ...(header && token ? { [header]: token } : {})
                },
                body: JSON.stringify({ event_id: eventId })
            });

            const text = await response.text();

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    openLoginPrompt(
                        "로그인이 필요해요",
                        "즐겨찾기는 로그인 후 이용할 수 있어요."
                    );
                    return;
                }

                alert(text || "즐겨찾기 처리에 실패했습니다.");
                return;
            }

            updateFavoriteButton(!isFavorite);
        } catch (error) {
            console.error("즐겨찾기 처리 실패:", error);
            alert("서버 오류가 발생했습니다.");
        }
    }

    function renderReviews(reviews) {
        if (!Array.isArray(reviews) || reviews.length === 0) {
            reviewList.innerHTML = `<div class="review-empty">아직 작성된 리뷰가 없어요. 첫 리뷰를 남겨보세요.</div>`;
            return;
        }

        reviewList.innerHTML = reviews.map(review => {
            const canEdit = currentUsername && review.username === currentUsername;

            return `
                <article class="review-item" data-review-id="${review.id}">
                    <div class="review-item__top">
                        <span class="review-item__author">${escapeHtml(review.username)}</span>
                        <span class="review-item__date">${escapeHtml(formatDate(review.createdAt))}</span>
                    </div>
                    <div class="review-item__content">${escapeHtml(review.content).replace(/\n/g, "<br>")}</div>
                    ${canEdit ? `
                        <div class="review-item__actions">
                            <button type="button" class="review-edit-btn">수정</button>
                            <button type="button" class="review-delete-btn">삭제</button>
                        </div>
                    ` : ""}
                </article>
            `;
        }).join("");
    }

    async function loadReviews() {
        try {
            const response = await fetch(`/festivals/${festivalId}/reviews`, {
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error("리뷰 조회 실패");
            }

            const reviews = await response.json();
            renderReviews(reviews);
        } catch (error) {
            console.error("리뷰 불러오기 실패:", error);
            reviewList.innerHTML = `<div class="review-empty">리뷰를 불러오지 못했습니다.</div>`;
        }
    }

    async function submitReview(event) {
        event.preventDefault();

        if (!isLoggedIn) {
            openLoginPrompt(
                "로그인이 필요해요",
                "리뷰 작성은 로그인 후 이용할 수 있어요."
            );
            return;
        }

        const content = reviewContent.value.trim();
        if (!content) {
            alert("리뷰 내용을 입력해주세요.");
            reviewContent.focus();
            return;
        }

        try {
            const response = await fetch(`/festivals/${festivalId}/reviews`, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    ...(header && token ? { [header]: token } : {})
                },
                body: JSON.stringify({ content })
            });

            const result = await response.json();

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    openLoginPrompt(
                        "로그인이 필요해요",
                        "리뷰 작성은 로그인 후 이용할 수 있어요."
                    );
                    return;
                }

                alert(result.message || "리뷰 등록에 실패했습니다.");
                return;
            }

            reviewContent.value = "";
            await loadReviews();
        } catch (error) {
            console.error("리뷰 등록 실패:", error);
            alert("서버 오류가 발생했습니다.");
        }
    }

    async function deleteReview(reviewId) {
        try {
            const response = await fetch(`/festivals/${festivalId}/reviews/${reviewId}`, {
                method: "DELETE",
                credentials: "same-origin",
                headers: {
                    ...(header && token ? { [header]: token } : {})
                }
            });

            const result = await response.json();

            if (!response.ok) {
                alert(result.message || "리뷰 삭제에 실패했습니다.");
                return;
            }

            await loadReviews();
        } catch (error) {
            console.error("리뷰 삭제 실패:", error);
            alert("서버 오류가 발생했습니다.");
        }
    }

    async function updateReview(reviewId, content) {
        try {
            const response = await fetch(`/festivals/${festivalId}/reviews/${reviewId}`, {
                method: "PATCH",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    ...(header && token ? { [header]: token } : {})
                },
                body: JSON.stringify({ content })
            });

            const result = await response.json();

            if (!response.ok) {
                alert(result.message || "리뷰 수정에 실패했습니다.");
                return false;
            }

            await loadReviews();
            return true;
        } catch (error) {
            console.error("리뷰 수정 실패:", error);
            alert("서버 오류가 발생했습니다.");
            return false;
        }
    }

    reviewList?.addEventListener("click", async (event) => {
        const reviewItem = event.target.closest(".review-item");
        if (!reviewItem) return;

        const reviewId = reviewItem.dataset.reviewId;

        if (event.target.classList.contains("review-delete-btn")) {
            const ok = confirm("리뷰를 삭제할까요?");
            if (ok) {
                await deleteReview(reviewId);
            }
            return;
        }

        if (event.target.classList.contains("review-edit-btn")) {
            const existingEditArea = reviewItem.querySelector(".review-edit-area");
            if (existingEditArea) return;

            const contentEl = reviewItem.querySelector(".review-item__content");
            const currentContent = contentEl.innerText.trim();

            const actionsEl = reviewItem.querySelector(".review-item__actions");
            if (actionsEl) {
                actionsEl.remove();
            }

            reviewItem.insertAdjacentHTML("beforeend", `
                <div class="review-edit-area">
                    <textarea class="review-edit-textarea">${escapeHtml(currentContent)}</textarea>
                    <div class="review-item__actions">
                        <button type="button" class="review-save-btn">수정 저장</button>
                        <button type="button" class="review-cancel-btn">취소</button>
                    </div>
                </div>
            `);

            reviewItem.querySelector(".review-edit-textarea")?.focus();
            return;
        }

        if (event.target.classList.contains("review-cancel-btn")) {
            await loadReviews();
            return;
        }

        if (event.target.classList.contains("review-save-btn")) {
            const editTextarea = reviewItem.querySelector(".review-edit-textarea");
            const newContent = editTextarea?.value.trim() || "";

            if (!newContent) {
                alert("리뷰 내용을 입력해주세요.");
                editTextarea?.focus();
                return;
            }

            await updateReview(reviewId, newContent);
        }
    });

    loginPromptClose?.addEventListener("click", closeLoginPrompt);
    loginPromptCancel?.addEventListener("click", closeLoginPrompt);
    loginPromptModal?.addEventListener("click", (event) => {
        if (event.target.classList.contains("login-prompt-modal__backdrop")) {
            closeLoginPrompt();
        }
    });

    reviewForm?.addEventListener("submit", submitReview);
    favoriteToggleBtn?.addEventListener("click", toggleFavorite);

    if (!isLoggedIn) {
        applyGuestView();
    }

    loadReviews();
    loadFavoriteState();
});

function initializeDetailImageLightbox() {
    const trigger = document.getElementById('detailImageTrigger');
    const heroImage = document.getElementById('detailHeroImage');
    const lightbox = document.getElementById('imageLightbox');
    const lightboxImg = document.getElementById('imageLightboxImg');
    const closeBtn = document.getElementById('imageLightboxClose');
    const backdrop = document.getElementById('imageLightboxBackdrop');

    if (!trigger || !heroImage || !lightbox || !lightboxImg || !closeBtn || !backdrop) {
        return;
    }

    function openLightbox() {
        lightboxImg.src = heroImage.src;
        lightboxImg.alt = heroImage.alt || '확대 이미지';
        lightbox.classList.add('is-open');
        lightbox.setAttribute('aria-hidden', 'false');
        document.body.classList.add('lightbox-open');
    }

    function closeLightbox() {
        lightbox.classList.remove('is-open');
        lightbox.setAttribute('aria-hidden', 'true');
        lightboxImg.src = '';
        document.body.classList.remove('lightbox-open');
    }

    trigger.addEventListener('click', openLightbox);
    closeBtn.addEventListener('click', closeLightbox);
    backdrop.addEventListener('click', closeLightbox);

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && lightbox.classList.contains('is-open')) {
            closeLightbox();
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    initializeDetailImageLightbox();
});