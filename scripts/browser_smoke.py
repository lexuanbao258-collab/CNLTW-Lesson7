from pathlib import Path
import re
from playwright.sync_api import sync_playwright, expect

BASE = "http://localhost:18080"
OUT = Path("build/browser-results")
OUT.mkdir(parents=True, exist_ok=True)
checks = []

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    page.route("https://**/*", lambda route: route.abort())
    page.goto(BASE)
    page.wait_for_load_state("networkidle")
    expect(page.get_by_role("heading", name="Khóa học rõ ràng.")).to_be_visible()
    page.screenshot(path=str(OUT / "home.png"), full_page=True)
    (OUT / "home-dom.html").write_text(page.content(), encoding="utf-8")
    checks.append("Trang chủ, navbar và DOM thực tế")

    def category(name):
        page.goto(BASE + "/categories/new")
        page.get_by_label("Tên loại khóa học").fill(name)
        page.get_by_role("button", name="Lưu loại khóa học").click()
        page.wait_for_load_state("networkidle")
        row = page.get_by_role("row").filter(has=page.get_by_role("link", name=name, exact=True))
        return int(row.locator("td").first.inner_text())

    web = category("Web kiểm thử")
    backend = category("Backend kiểm thử")
    checks.append("Thêm và hiển thị category từ database")

    def course(name, category_id, fee, hours, active):
        page.goto(BASE + "/courses/new")
        page.get_by_label("Tên khóa học").fill(name)
        page.get_by_label("Loại khóa học").select_option(str(category_id))
        page.get_by_label("Học phí").fill(str(fee))
        page.get_by_label("Thời lượng").fill(str(hours))
        page.get_by_label("Đang tuyển sinh").set_checked(active)
        page.get_by_label("Mô tả").fill("Nội dung kiểm thử trình duyệt")
        page.get_by_role("button", name="Lưu khóa học").click()
        page.wait_for_load_state("networkidle")
        expect(page.get_by_role("heading", name=name, exact=True)).to_be_visible()
        return int(page.url.rsplit("/", 1)[1])

    spring = course("Spring Browser", web, 3000000, 48, True)
    react = course("React Browser", web, 1800000, 30, False)
    sql = course("SQL Browser", backend, 1200000, 20, True)
    expect(page.get_by_text("Khóa học chưa có ảnh.", exact=False)).to_be_visible()
    checks.append("Create Course, trạng thái và thông báo chưa có ảnh")

    page.goto(BASE + "/courses")
    expect(page.get_by_text("Ngắn hạn", exact=True)).to_have_count(2)
    expect(page.get_by_text("Dài hạn", exact=True)).to_have_count(1)
    page.get_by_label("Tên khóa học").fill("Spring")
    page.get_by_label("Loại khóa học").select_option(str(web))
    page.get_by_label("Trạng thái").select_option("true")
    page.get_by_label("Học phí tối thiểu").fill("2000000")
    page.get_by_label("Học phí tối đa").fill("3500000")
    page.get_by_label("Sắp xếp").select_option("feeDesc")
    page.get_by_role("button", name="Áp dụng bộ lọc").click()
    expect(page.locator("a.course-name")).to_have_text(["Spring Browser"])
    expect(page.get_by_label("Tên khóa học")).to_have_value("Spring")
    expect(page.get_by_label("Loại khóa học")).to_have_value(str(web))
    page.screenshot(path=str(OUT / "course-list.png"), full_page=True)
    checks.append("Kết hợp tìm tên/category/trạng thái/học phí/sort và giữ bộ lọc")

    for mode, expected in [
        ("feeAsc", ["SQL Browser", "React Browser", "Spring Browser"]),
        ("feeDesc", ["Spring Browser", "React Browser", "SQL Browser"]),
        ("durationAsc", ["SQL Browser", "React Browser", "Spring Browser"]),
    ]:
        page.goto(BASE + "/courses?sort=" + mode)
        expect(page.locator("a.course-name")).to_have_text(expected)
    page.goto(BASE + "/courses?isActive=false")
    expect(page.locator("a.course-name")).to_have_text(["React Browser"])
    page.goto(BASE + "/courses?minFee=500&maxFee=100")
    expect(page.get_by_text("Học phí tối thiểu không được lớn hơn học phí tối đa.", exact=True)).to_be_visible()
    page.goto(BASE + "/courses?name=no-results")
    expect(page.get_by_text("Không tìm thấy khóa học phù hợp", exact=True)).to_be_visible()
    checks.append("Ba kiểu sort, lọc ngừng tuyển sinh, khoảng phí sai và không kết quả")

    page.goto(BASE + f"/courses/{spring}/edit")
    page.get_by_label("Tên khóa học").fill("Spring Browser Updated")
    page.get_by_role("button", name="Lưu khóa học").click()
    expect(page.get_by_role("heading", name="Spring Browser Updated")).to_be_visible()
    page.goto(BASE + "/courses/new")
    page.get_by_label("Tên khóa học").fill("Spring Browser Updated")
    page.get_by_label("Loại khóa học").select_option(str(web))
    page.get_by_label("Học phí").fill("100")
    page.get_by_label("Thời lượng").fill("10")
    page.get_by_role("button", name="Lưu khóa học").click()
    expect(page.get_by_text("Tên khóa học đã tồn tại trong loại khóa học này.", exact=True)).to_be_visible()
    checks.append("Edit và lỗi tên khóa học trùng ngay trên form")

    page.goto(BASE + f"/courses/{spring}")
    page.get_by_label("URL hình ảnh").fill("ftp://example.com/no.jpg")
    page.get_by_role("button", name="Thêm ảnh", exact=True).click()
    expect(page.get_by_text("URL ảnh phải hợp lệ", exact=False)).to_be_visible()
    for url in ["https://example.com/one.jpg", "https://example.com/two.jpg"]:
        page.get_by_label("URL hình ảnh").fill(url)
        page.get_by_role("button", name="Thêm ảnh", exact=True).click()
        page.wait_for_load_state("networkidle")
    expect(page.get_by_text("Không tải được ảnh", exact=True)).to_have_count(2)
    expect(page.get_by_text("Ảnh đại diện", exact=True)).to_have_count(1)
    page.get_by_role("button", name="Đặt làm ảnh đại diện").click()
    expect(page.locator(".image-card").nth(1).get_by_text("Ảnh đại diện", exact=True)).to_be_visible()
    expect(page.get_by_text("Ảnh đại diện", exact=True)).to_have_count(1)
    page.screenshot(path=str(OUT / "course-detail.png"), full_page=True)
    checks.append("Validation URL, nhiều ảnh, fallback lỗi tải và đổi thumbnail")

    page.on("dialog", lambda dialog: dialog.accept())
    page.goto(BASE + "/categories")
    row = page.get_by_role("row").filter(has=page.get_by_role("link", name="Web kiểm thử", exact=True))
    row.get_by_role("button", name="Xóa", exact=True).click()
    expect(page.get_by_text("Không thể xóa loại khóa học vì đang có khóa học thuộc loại này.", exact=True)).to_be_visible()
    page.goto(BASE + f"/categories/{backend}/edit")
    page.get_by_label("Tên loại khóa học").fill("Backend cập nhật")
    page.get_by_role("button", name="Lưu loại khóa học").click()
    expect(page.get_by_role("link", name="Backend cập nhật")).to_be_visible()
    empty = category("Loại trống")
    page.goto(BASE + "/categories")
    page.get_by_role("row").filter(has=page.get_by_role("link", name="Loại trống", exact=True)).get_by_role("button", name="Xóa").click()
    expect(page.get_by_role("link", name="Loại trống", exact=True)).to_have_count(0)
    checks.append("Sửa/xóa Category và chặn xóa category đang sử dụng")

    page.goto(BASE + "/courses")
    page.get_by_role("row").filter(has=page.get_by_role("link", name="React Browser", exact=True)).get_by_role("button", name="Xóa", exact=True).click()
    expect(page.locator("a.course-name")).to_have_count(2)
    response = page.goto(BASE + "/courses/999999999")
    assert response.status == 404
    expect(page.get_by_text("Không tìm thấy khóa học.", exact=True)).to_be_visible()
    page.screenshot(path=str(OUT / "not-found.png"), full_page=True)
    checks.append("Xóa Course và trang 404 thân thiện")

    page.goto(BASE + "/courses")
    page.set_viewport_size({"width": 390, "height": 844})
    page.screenshot(path=str(OUT / "mobile.png"), full_page=True)
    expect(page.get_by_role("button", name="Áp dụng bộ lọc")).to_be_visible()
    checks.append("Giao diện 1440px và 390px")
    browser.close()

report = "\n".join(f"PASS {i+1}. {name}" for i, name in enumerate(checks))
(OUT / "results.txt").write_text(report + "\n", encoding="utf-8")
print(f"PASS: {len(checks)} browser check groups. See build/browser-results/results.txt")
print("Browser tests use isolated H2; remote image failures are deliberately simulated.")
