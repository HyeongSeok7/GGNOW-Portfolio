document.addEventListener("DOMContentLoaded", function () {

    const sortSelect = document.getElementById("serverSortOption");
    const pageSizeSelect = document.getElementById("serverPageSize");
    const endedCategoryFilter = document.getElementById("endedCategoryFilter");

    function movePage(params) {
        const url = new URL(window.location.href);

        Object.keys(params).forEach(function (key) {
            url.searchParams.set(key, params[key]);
        });

        url.searchParams.set("page", "0");

        window.location.href = url.toString();
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", function () {
            movePage({
                sort: sortSelect.value
            });
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener("change", function () {
            movePage({
                size: pageSizeSelect.value
            });
        });
    }

    if (endedCategoryFilter) {
        endedCategoryFilter.addEventListener("change", function () {
            movePage({
                category: endedCategoryFilter.value
            });
        });
    }
});