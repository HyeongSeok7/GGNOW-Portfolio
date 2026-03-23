document.addEventListener("DOMContentLoaded", () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    document.querySelectorAll(".favorite-remove-btn").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            e.preventDefault();
            e.stopPropagation();

            const eventId = btn.getAttribute("data-event-id");
            if (!eventId) {
                alert("event_id가 없습니다. (data-event-id 확인)");
                return;
            }

            try {
                const headers = { "Content-Type": "application/json" };
                if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

                const res = await fetch("/removeFavoriteEvent", {
                    method: "DELETE",
                    headers,
                    body: JSON.stringify({ event_id: eventId })
                });

                if (!res.ok) {
                    const text = await res.text();
                    alert("해제 실패: " + text);
                    return;
                }

                // ✅ 화면에서 카드 제거
                const card = btn.closest(".event-card");
                if (card) card.remove();

                // ✅ 모두 제거되면 빈 상태 메시지 보여주기(선택)
                const list = document.querySelector(".favorites-section");
                const remaining = document.querySelectorAll(".event-card").length;
                if (list && remaining === 0) {
                    list.insertAdjacentHTML("beforeend", "<p>즐겨찾기한 행사가 없습니다.</p>");
                }

            } catch (err) {
                console.error(err);
                alert("요청 중 오류가 발생했습니다.");
            }
        });
    });
});