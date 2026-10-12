document.querySelectorAll('img').forEach(image => {
    const showFallback = () => {
        const fallback = document.createElement('span');
        fallback.className = 'image-fallback';
        fallback.textContent = 'Không tải được ảnh';
        image.replaceWith(fallback);
    };
    image.addEventListener('error', showFallback, { once: true });
    if (image.complete && image.naturalWidth === 0) showFallback();
});
document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
});
