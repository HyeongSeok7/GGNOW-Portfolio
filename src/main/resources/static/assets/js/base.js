// 로그인 상태 확인
fetch('/check-login')
    .then(response => response.text())
    .then(data => {
        const authSection = document.getElementById("authSection");
        const stars = document.querySelectorAll(".star-icon");

        if (!authSection) return;

        if (data.includes("로그인 상태입니다")) {
            authSection.innerHTML = `
                <a href="/mypage" class="auth-btn"><span>마이페이지</span></a>
                <button type="button" id="logoutBtn" class="auth-btn"><span>로그아웃</span></button>
            `;

            const logoutBtn = document.getElementById("logoutBtn");
            logoutBtn?.addEventListener("click", () => {
                const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
                const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");

                fetch("/logout", {
                    method: "POST",
                    credentials: "same-origin",
                    headers: header && token ? { [header]: token } : {}
                })
                    .then(() => {
                        window.location.href = "/main";
                    })
                    .catch(err => {
                        console.error("로그아웃 오류:", err);
                        window.location.href = "/main";
                    });
            });
        } else {
            authSection.innerHTML = `<a href="/login"><span>로그인 / 회원가입</span></a>`;
            stars.forEach(star => {
                star.style.display = "none";
            });
        }
    })
    .catch(error => {
        console.error("오류 발생:", error);
        const authSection = document.getElementById("authSection");
        if (authSection) {
            authSection.innerText = "로그인 상태를 확인할 수 없습니다.";
        }
    });

function parseDate(str) {
    if (!str) return new Date('2100-01-01');
    if (str.includes('-')) return new Date(str);

    if (str.length === 8) {
        const year = str.substring(0, 4);
        const month = str.substring(4, 6);
        const day = str.substring(6, 8);
        return new Date(`${year}-${month}-${day}`);
    }

    return new Date('2100-01-01');
}

function sortEvents() {
    const sortSelect = document.getElementById('sortOption');
    const list = document.getElementById('eventList');

    if (!sortSelect || !list) return;

    const option = sortSelect.value;
    const cards = Array.from(list.querySelectorAll('.festival-card'));

    cards.sort((a, b) => {
        const startA = parseDate(a.getAttribute('data-start'));
        const startB = parseDate(b.getAttribute('data-start'));
        const endA = parseDate(a.getAttribute('data-end'));
        const endB = parseDate(b.getAttribute('data-end'));

        if (option === 'latest') {
            return startB - startA;
        }

        return endA - endB;
    });

    cards.forEach(card => list.appendChild(card));
}

document.addEventListener('DOMContentLoaded', function () {
    const stars = document.querySelectorAll('.star-icon');
    const sortSelect = document.getElementById('sortOption');
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");

    if (sortSelect) {
        sortSelect.addEventListener('change', sortEvents);
        sortEvents();
    }

    document.querySelectorAll('.festival-card h2').forEach(h2 => {
        h2.textContent = h2.textContent.replace(/\s+/g, ' ').trim();
    });

    if (stars.length === 0) return;

    fetch('/check-login')
        .then(response => response.text())
        .then(data => {
            if (!data.includes("로그인 상태입니다")) {
                stars.forEach(star => {
                    star.style.display = "none";
                });
                return;
            }

            fetch('/getFavoriteEvents')
                .then(response => response.json())
                .then(favoriteEventIds => {
                    stars.forEach(star => {
                        const eventId = star.dataset.eventId;
                        if (favoriteEventIds.includes(eventId)) {
                            star.classList.add('filled');
                        }
                    });
                })
                .catch(error => console.error('Error loading favorites:', error));

            stars.forEach(star => {
                star.addEventListener('click', function () {
                    const eventId = this.dataset.eventId;
                    const isFilled = this.classList.contains('filled');
                    const url = isFilled ? '/removeFavoriteEvent' : '/addFavoriteEvent';
                    const method = isFilled ? 'DELETE' : 'POST';

                    fetch(url, {
                        method,
                        credentials: "same-origin",
                        headers: {
                            'Content-Type': 'application/json',
                            ...(header && token ? { [header]: token } : {})
                        },
                        body: JSON.stringify({ event_id: eventId })
                    })
                        .then(response => {
                            if (response.ok) {
                                this.classList.toggle('filled');
                            } else {
                                alert('작업 실패');
                            }
                        })
                        .catch(error => {
                            console.error('Error:', error);
                            alert('서버 오류');
                        });
                });
            });
        });
});