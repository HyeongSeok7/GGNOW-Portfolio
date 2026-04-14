console.log('BASE_JS_FILE_LOADED_v4_0');

function getCsrfInfo() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');

    return {
        token: tokenMeta ? tokenMeta.getAttribute('content') : '',
        header: headerMeta ? headerMeta.getAttribute('content') : ''
    };
}

function createCsrfHeaders(baseHeaders) {
    const csrf = getCsrfInfo();
    const headers = baseHeaders ? { ...baseHeaders } : {};

    if (csrf.header && csrf.token) {
        headers[csrf.header] = csrf.token;
    }

    return headers;
}

function parseDate(str) {
    if (!str) return new Date('2100-01-01');

    if (str.indexOf('-') > -1) {
        return new Date(str);
    }

    if (str.length === 8) {
        const year = str.substring(0, 4);
        const month = str.substring(4, 6);
        const day = str.substring(6, 8);
        return new Date(year + '-' + month + '-' + day);
    }

    return new Date('2100-01-01');
}

function sortEvents(animate) {
    const sortSelect = document.getElementById('sortOption');
    const list = document.getElementById('eventList');

    if (!sortSelect || !list) return;

    const option = sortSelect.value;
    const cards = Array.from(list.querySelectorAll('.festival-card'));

    if (!cards.length) return;

    cards.sort(function (a, b) {
        const startA = parseDate(a.getAttribute('data-start'));
        const startB = parseDate(b.getAttribute('data-start'));
        const endA = parseDate(a.getAttribute('data-end'));
        const endB = parseDate(b.getAttribute('data-end'));

        if (option === 'latest') {
            return startB - startA;
        }

        return endA - endB;
    });

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (!animate || reduceMotion) {
        cards.forEach(function (card) {
            list.appendChild(card);
        });

        if (!reduceMotion) {
            initializeFestivalCardReveal();
        } else {
            cards.forEach(function (card) {
                card.classList.add('is-revealed');
            });
        }
        return;
    }

    cards.forEach(function (card) {
        card.classList.remove('is-revealed');
    });

    setTimeout(function () {
        cards.forEach(function (card) {
            list.appendChild(card);
        });

        initializeFestivalCardReveal();
    }, 180);
}

function normalizeFestivalTitles() {
    const titles = document.querySelectorAll('.festival-card h2');

    titles.forEach(function (title) {
        title.textContent = title.textContent.replace(/\s+/g, ' ').trim();
    });
}

function bindLogoutButton() {
    const logoutBtn = document.getElementById('logoutBtn');

    if (!logoutBtn) return;

    logoutBtn.addEventListener('click', function () {
        fetch('/logout', {
            method: 'POST',
            credentials: 'same-origin',
            headers: createCsrfHeaders({})
        })
            .then(function () {
                window.location.href = '/main';
            })
            .catch(function (err) {
                console.error('로그아웃 오류:', err);
                window.location.href = '/main';
            });
    });
}

function updateAuthSection(isLoggedIn) {
    const authSection = document.getElementById('authSection');
    const stars = document.querySelectorAll('.star-icon');

    if (!authSection) return;

    if (isLoggedIn) {
        authSection.innerHTML =
            '<a href="/mypage" class="auth-btn"><span>마이페이지</span></a>' +
            '<button type="button" id="logoutBtn" class="auth-btn"><span>로그아웃</span></button>';

        bindLogoutButton();
        return;
    }

    authSection.innerHTML = '<a href="/login"><span>로그인 / 회원가입</span></a>';

    stars.forEach(function (star) {
        star.style.display = 'none';
    });
}

function checkLoginStatus() {
    return fetch('/check-login', {
        credentials: 'same-origin'
    })
        .then(function (response) {
            return response.text();
        })
        .then(function (data) {
            return data.indexOf('로그인 상태입니다') > -1;
        });
}

function initializeAuth() {
    checkLoginStatus()
        .then(function (isLoggedIn) {
            updateAuthSection(isLoggedIn);
        })
        .catch(function (error) {
            console.error('오류 발생:', error);

            const authSection = document.getElementById('authSection');
            if (authSection) {
                authSection.innerText = '로그인 상태를 확인할 수 없습니다.';
            }
        });
}

function initializeFavorites() {
    const stars = document.querySelectorAll('.star-icon');

    if (!stars.length) return;

    checkLoginStatus()
        .then(function (isLoggedIn) {
            if (!isLoggedIn) {
                stars.forEach(function (star) {
                    star.style.display = 'none';
                });
                return;
            }

            fetch('/getFavoriteEvents', {
                credentials: 'same-origin'
            })
                .then(function (response) {
                    return response.json();
                })
                .then(function (favoriteEventIds) {
                    stars.forEach(function (star) {
                        const eventId = star.dataset.eventId;
                        if (favoriteEventIds.includes(eventId)) {
                            star.classList.add('filled');
                        }
                    });
                })
                .catch(function (error) {
                    console.error('Error loading favorites:', error);
                });

            stars.forEach(function (star) {
                star.addEventListener('click', function () {
                    const eventId = this.dataset.eventId;
                    const isFilled = this.classList.contains('filled');
                    const url = isFilled ? '/removeFavoriteEvent' : '/addFavoriteEvent';
                    const method = isFilled ? 'DELETE' : 'POST';

                    fetch(url, {
                        method: method,
                        credentials: 'same-origin',
                        headers: createCsrfHeaders({
                            'Content-Type': 'application/json'
                        }),
                        body: JSON.stringify({ event_id: eventId })
                    })
                        .then(function (response) {
                            if (response.ok) {
                                star.classList.toggle('filled');
                            } else {
                                alert('작업 실패');
                            }
                        })
                        .catch(function (error) {
                            console.error('Error:', error);
                            alert('서버 오류');
                        });
                });
            });
        })
        .catch(function (error) {
            console.error('즐겨찾기 초기화 오류:', error);
        });
}

function initializeSort() {
    const sortSelect = document.getElementById('sortOption');

    if (!sortSelect) return;

    sortEvents(false);

    sortSelect.addEventListener('change', function () {
        sortEvents(true);
    });
}

function initializeScrollHeader() {
    function onScroll() {
        if (window.scrollY > 12) {
            document.body.classList.add('is-scrolled');
        } else {
            document.body.classList.remove('is-scrolled');
        }
    }

    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
}

function initializeInteractiveCards() {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion) return;

    const cards = document.querySelectorAll('.dreamy-festival-card, .result-card');

    cards.forEach(function (card) {
        let frameId = null;

        function resetCard() {
            card.style.transform = '';
            card.style.setProperty('--card-glow-x', '50%');
            card.style.setProperty('--card-glow-y', '50%');
        }

        card.addEventListener('mousemove', function (event) {
            const rect = card.getBoundingClientRect();
            const offsetX = event.clientX - rect.left;
            const offsetY = event.clientY - rect.top;

            const centerX = rect.width / 2;
            const centerY = rect.height / 2;

            const rotateY = ((offsetX - centerX) / centerX) * 4;
            const rotateX = ((centerY - offsetY) / centerY) * 3;

            if (frameId) cancelAnimationFrame(frameId);

            frameId = requestAnimationFrame(function () {
                card.style.transform =
                    'translateY(-8px) perspective(900px) rotateX(' +
                    rotateX.toFixed(2) +
                    'deg) rotateY(' +
                    rotateY.toFixed(2) +
                    'deg) scale(1.012)';

                card.style.setProperty('--card-glow-x', offsetX + 'px');
                card.style.setProperty('--card-glow-y', offsetY + 'px');
            });
        });

        card.addEventListener('mouseleave', function () {
            if (frameId) cancelAnimationFrame(frameId);
            resetCard();
        });
    });
}

function initializeMotion() {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const motionGroups = [
        '.hero-card',
    	'.section-intro',
    	'.search-panel',
    	'.category-section',
    	'.category-card',
    	'.home-info-panels .info-panel',
    	'.event-hero',
    	'.event-search-panel',
    	'.result-card',
    	'.customer-hero-card',
    	'.faq-card',
    	'.notice-card'
    ];

    const motionTargets = document.querySelectorAll(motionGroups.join(','));

    if (!motionTargets.length) return;

    document.body.classList.add('motion-ready');

    motionTargets.forEach(function (el, index) {
        el.classList.add('motion-target');

        const parentGrid = el.closest(
            '.category-grid, .dreamy-event-grid, .home-info-panels, .portfolio-result-list'
        );

        if (parentGrid) {
            const siblings = Array.from(parentGrid.children).filter(function (node) {
                return node.classList && !node.classList.contains('empty-state');
            });

            const siblingIndex = siblings.indexOf(el);

            if (siblingIndex > -1) {
                el.setAttribute('data-motion-delay', String((siblingIndex % 6) + 1));
            }
        } else {
            el.setAttribute('data-motion-delay', String((index % 4) + 1));
        }
    });

    if (reduceMotion) {
        motionTargets.forEach(function (el) {
            el.classList.add('is-visible');
        });
        return;
    }

    if (!('IntersectionObserver' in window)) {
        motionTargets.forEach(function (el) {
            el.classList.add('is-visible');
        });
        return;
    }

    const observer = new IntersectionObserver(
        function (entries, obs) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;

                entry.target.classList.add('is-visible');
                obs.unobserve(entry.target);
            });
        },
        {
            root: null,
            rootMargin: '0px 0px -12% 0px',
            threshold: 0.28
        }
    );

    motionTargets.forEach(function (el) {
        observer.observe(el);
    });
}

document.addEventListener('DOMContentLoaded', function () {
    console.log('BASE_JS_DOM_READY_v4_0');

    if (document.getElementById('eventList')) {
        document.documentElement.classList.add('js-festival-reveal');
    }

    initializeAuth();
    normalizeFestivalTitles();
    initializeFavorites();
    initializeMotion();
    initializeScrollHeader();
    initializeInteractiveCards();
    initializeSort();
    initializeFestivalCardReveal();
});

let festivalCardObserver = null;

function prepareCardForReveal(card) {
    card.dataset.revealed = 'false';
    card.style.opacity = '0';
    card.style.transform = 'translateY(28px) scale(0.985)';
    card.style.filter = 'blur(8px)';
    card.style.pointerEvents = 'none';
    card.style.willChange = 'opacity, transform, filter';
}

function revealCard(card, delay) {
    if (!card || card.dataset.revealed === 'true') return;

    const wait = typeof delay === 'number' ? delay : 0;

    setTimeout(function () {
        card.dataset.revealed = 'true';
        card.style.pointerEvents = '';

        card.animate(
            [
                {
                    opacity: 0,
                    transform: 'translateY(28px) scale(0.985)',
                    filter: 'blur(8px)'
                },
                {
                    opacity: 1,
                    transform: 'translateY(0px) scale(1)',
                    filter: 'blur(0px)'
                }
            ],
            {
                duration: 650,
                easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
                fill: 'forwards'
            }
        );

        card.style.opacity = '1';
        card.style.transform = 'translateY(0px) scale(1)';
        card.style.filter = 'blur(0px)';
    }, wait);
}

function revealCardsSequentially(cards, step) {
    cards.forEach(function (card, index) {
        revealCard(card, index * step);
    });
}


function initializeFestivalCardReveal() {
    const list = document.getElementById('eventList');
    if (!list) return;

    const cards = Array.from(list.querySelectorAll('.festival-card, .dreamy-festival-card'));
    if (!cards.length) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (festivalCardObserver) {
        festivalCardObserver.disconnect();
        festivalCardObserver = null;
    }

    if (reduceMotion) {
        cards.forEach(function (card) {
            card.style.opacity = '1';
            card.style.transform = 'none';
            card.style.filter = 'none';
            card.style.pointerEvents = '';
            card.dataset.revealed = 'true';
        });
        return;
    }

    cards.forEach(function (card) {
        prepareCardForReveal(card);
    });

    festivalCardObserver = new IntersectionObserver(
        function (entries, obs) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;

                const card = entry.target;
                const allCards = Array.from(list.querySelectorAll('.festival-card, .dreamy-festival-card'));
                const index = allCards.indexOf(card);

                revealCard(card, (index % 4) * 90);
                obs.unobserve(card);
            });
        },
        {
            root: null,
            threshold: 0.18,
            rootMargin: '0px 0px -8% 0px'
        }
    );

    cards.forEach(function (card) {
        festivalCardObserver.observe(card);
    });
}