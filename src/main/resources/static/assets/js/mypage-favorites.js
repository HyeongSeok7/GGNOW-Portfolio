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

                // 현재 category를 유지하면서 개수와 빈 화면 안내를 함께 갱신한다.
				window.location.reload();

            } catch (err) {
                console.error(err);
                alert("요청 중 오류가 발생했습니다.");
            }
        });
    });
});