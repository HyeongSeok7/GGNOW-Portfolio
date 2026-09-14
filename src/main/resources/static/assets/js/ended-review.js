document.addEventListener("DOMContentLoaded", function () {

    const festivalId = document
        .querySelector('meta[name="festivalId"]')
        ?.getAttribute("content");

    const reviewList = document.getElementById("reviewList");

    if (!festivalId || !reviewList) {
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
        if (!value) {
            return "";
        }

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return value;
        }

        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, "0");
        const dd = String(date.getDate()).padStart(2, "0");
        const hh = String(date.getHours()).padStart(2, "0");
        const mi = String(date.getMinutes()).padStart(2, "0");

        return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
    }

    function renderReviews(reviews) {
        if (!Array.isArray(reviews) || reviews.length === 0) {
            reviewList.innerHTML = `
                <div class="review-empty">
                    이 행사에 작성된 리뷰가 없어요.
                </div>
            `;
            return;
        }

        reviewList.innerHTML = reviews.map(function (review) {
            return `
                <article class="review-item">
                    <div class="review-item__top">
                        <span class="review-item__author">
                            ${escapeHtml(review.username)}
                        </span>

                        <span class="review-item__date">
                            ${escapeHtml(formatDate(review.createdAt))}
                        </span>
                    </div>

                    <div class="review-item__content">
                        ${escapeHtml(review.content).replace(/\n/g, "<br>")}
                    </div>
                </article>
            `;
        }).join("");
    }

    async function loadReviews() {
        try {
            const response = await fetch(
                `/festivals/${festivalId}/reviews`,
                {
                    credentials: "same-origin"
                }
            );

            if (!response.ok) {
                throw new Error("리뷰 조회 실패");
            }

            const reviews = await response.json();

            renderReviews(reviews);

        } catch (error) {
            console.error("종료 행사 리뷰 불러오기 실패:", error);

            reviewList.innerHTML = `
                <div class="review-empty">
                    리뷰를 불러오지 못했습니다.
                </div>
            `;
        }
    }

    loadReviews();
});