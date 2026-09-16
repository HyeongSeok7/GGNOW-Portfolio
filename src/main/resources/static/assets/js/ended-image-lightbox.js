document.addEventListener("DOMContentLoaded", () => {
    const trigger = document.getElementById("detailImageTrigger");
    const heroImage = document.getElementById("detailHeroImage");
    const lightbox = document.getElementById("imageLightbox");
    const lightboxImage = document.getElementById("imageLightboxImg");
    const closeButton = document.getElementById("imageLightboxClose");
    const backdrop = document.getElementById("imageLightboxBackdrop");

    if (!trigger || !heroImage || !lightbox ||
        !lightboxImage || !closeButton || !backdrop) {
        return;
    }

    function openLightbox() {
        const imageUrl = heroImage.currentSrc || heroImage.src;
        if (!imageUrl) return;

        lightboxImage.src = imageUrl;
        lightboxImage.alt = heroImage.alt || "행사 확대 이미지";

        lightbox.classList.add("is-open");
        lightbox.setAttribute("aria-hidden", "false");

        // 확대 창이 열린 동안 배경 페이지의 스크롤을 막는다.
        document.body.classList.add("lightbox-open");

        closeButton.focus();
    }

    function closeLightbox() {
        if (!lightbox.classList.contains("is-open")) return;

        lightbox.classList.remove("is-open");
        document.body.classList.remove("lightbox-open");

        // 닫은 뒤 원래 이미지 버튼으로 키보드 포커스를 돌려준다.
        trigger.focus();

        lightbox.setAttribute("aria-hidden", "true");
        lightboxImage.removeAttribute("src");
    }

    // 이미지 클릭으로 열기
    trigger.addEventListener("click", openLightbox);

    // 닫기 버튼 또는 어두운 배경 클릭으로 닫기
    closeButton.addEventListener("click", closeLightbox);
    backdrop.addEventListener("click", closeLightbox);

    document.addEventListener("keydown", (event) => {
        if (!lightbox.classList.contains("is-open")) return;

        if (event.key === "Escape") {
            event.preventDefault();
            closeLightbox();
        } else if (event.key === "Tab") {
            // 확대 창의 유일한 버튼에 포커스를 유지한다.
            event.preventDefault();
            closeButton.focus();
        }
    });
});