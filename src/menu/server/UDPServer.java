package menu.server;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Server UDP - Quản lý thực đơn (loại món & món ăn).
 * Giao thức văn bản: yêu cầu "LỆNH|trường1|trường2...", trả lời "OK\nthông báo\n..." hoặc "ERR\nthông báo".
 * Với lệnh sửa (CAT_UPDATE, DISH_UPDATE), dòng 3 là bản ghi TRƯỚC khi sửa, dòng 4 là bản ghi SAU khi sửa.
 */
public class UDPServer {
    static final int PORT = 9999;
    static final String ID_REGEX = "[A-Za-z0-9_-]{1,20}";
    static final String NUM_REGEX = "[0-9]{1,13}";
    static final String TIME_REGEX = "[0-9]{1,4}";
    static final String ID_MSG = " chỉ gồm chữ không dấu, số, '_' hoặc '-' (tối đa 20 ký tự, không có khoảng trắng)";

    public static void main(String[] args) throws Exception {
        try {
            DBConnection.init();
            System.out.println("Ket noi MySQL thanh cong (database: " + DBConnection.DB + ")");
        } catch (SQLException e) {
            System.out.println("KHONG ket noi duoc MySQL: " + e.getMessage());
            System.out.println("-> Hay bat MySQL trong XAMPP Control Panel roi chay lai.");
            return;
        }

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("UDP Server dang chay tai cong " + PORT);
            byte[] buf = new byte[65000];
            while (true) {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                socket.receive(in);
                String req = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
                System.out.println("<< " + req);

                String res;
                try {
                    res = handle(req);
                } catch (SQLException e) {
                    res = err("Lỗi CSDL: " + e.getMessage());
                } catch (Exception e) {
                    res = err("Dữ liệu không hợp lệ: " + e.getMessage());
                }

                byte[] out = res.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(out, out.length, in.getAddress(), in.getPort()));
            }
        }
    }

    static String ok(String msg) { return "OK\n" + msg; }
    static String err(String msg) { return "ERR\n" + msg; }

    static String handle(String req) throws SQLException {
        if (req.matches("(?s).*[\\p{Cntrl}].*"))
            return err("Dữ liệu chứa ký tự điều khiển (xuống dòng, tab...) không hợp lệ");

        String[] p = req.split("\\|", -1);
        for (int i = 0; i < p.length; i++) p[i] = p[i].trim();

        // Số trường phải đúng chuẩn; dư trường nghĩa là dữ liệu có chứa ký tự '|'
        int expected;
        switch (p[0]) {
            case "CAT_ADD":  case "CAT_UPDATE":  expected = 4; break;
            case "DISH_ADD": case "DISH_UPDATE": expected = 7; break;
            case "DISH_SEARCH":                  expected = 6; break;
            case "CAT_DELETE": case "CAT_SEARCH": case "DISH_DELETE": expected = 2; break;
            default: return err("Lệnh không hợp lệ: " + p[0]);
        }
        if (p.length != expected) return err("Dữ liệu không đúng định dạng (không được chứa ký tự '|')");

        switch (p[0]) {
            case "CAT_ADD":     return catSave(p, true);
            case "CAT_UPDATE":  return catSave(p, false);
            case "CAT_DELETE":  return catDelete(p[1]);
            case "CAT_SEARCH":  return catSearch(p[1]);
            case "DISH_ADD":    return dishSave(p, true);
            case "DISH_UPDATE": return dishSave(p, false);
            case "DISH_DELETE": return dishDelete(p[1]);
            default:            return dishSearch(p);
        }
    }

    // ---------- tiện ích ----------
    static boolean exists(Connection c, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Escape ký tự đặc biệt của LIKE để từ khóa chứa % hoặc _ không khớp bừa. */
    static String like(String s) {
        return "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    static String nz(String s) { return s == null ? "" : s; }

    // ---------- LOẠI MÓN ----------
    /** Dòng dữ liệu: mã|tên|mô tả */
    static String catGet(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id,name,description FROM category WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) + "|" + rs.getString(2) + "|" + nz(rs.getString(3)) : null;
            }
        }
    }

    static String catSave(String[] p, boolean add) throws SQLException {
        String id = p[1], name = p[2], desc = p[3];
        if (!id.matches(ID_REGEX)) return err("Mã loại món" + ID_MSG);
        if (name.isEmpty() || name.length() > 100) return err("Tên loại món không được để trống và tối đa 100 ký tự");
        if (desc.length() > 255) return err("Mô tả tối đa 255 ký tự");

        try (Connection c = DBConnection.get()) {
            String before = catGet(c, id);
            if (add && before != null) return err("Mã loại món đã tồn tại");
            if (!add && before == null) return err("Không tìm thấy loại món: " + id);
            if (exists(c, "SELECT COUNT(*) FROM category WHERE name=? AND id<>?", name, id))
                return err("Tên loại món \"" + name + "\" đã tồn tại");

            String sql = add ? "INSERT INTO category(name,description,id) VALUES(?,?,?)"
                             : "UPDATE category SET name=?, description=? WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, desc);
                ps.setString(3, id);
                ps.executeUpdate();
            }
            if (add) return ok("Thêm loại món thành công: " + id + " - " + name);
            return ok("Sửa loại món thành công: " + id) + "\n" + before + "\n" + catGet(c, id);
        }
    }

    static String catDelete(String id) throws SQLException {
        if (id.isEmpty()) return err("Chưa nhập mã loại món");
        try (Connection c = DBConnection.get()) {
            if (!exists(c, "SELECT COUNT(*) FROM category WHERE id=?", id)) return err("Không tìm thấy loại món");
            if (exists(c, "SELECT COUNT(*) FROM dish WHERE category_id=?", id))
                return err("Loại món vẫn còn món ăn, phải xóa hoặc chuyển các món sang loại khác trước");
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM category WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        }
        return ok("Xóa loại món thành công: " + id);
    }

    static String catSearch(String kw) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id,name,description FROM category WHERE id LIKE ? OR name LIKE ? ORDER BY id")) {
            ps.setString(1, like(kw));
            ps.setString(2, like(kw));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append("\n").append(rs.getString(1)).append("|").append(rs.getString(2)).append("|")
                      .append(nz(rs.getString(3)));
                    n++;
                }
            }
        }
        return ok("Tìm thấy " + n + " loại món") + sb;
    }

    // ---------- MÓN ĂN ----------
    static final String DISH_SELECT = "SELECT d.id, d.category_id, c.name, d.name, d.price, d.serve_time, d.image "
            + "FROM dish d JOIN category c ON d.category_id = c.id ";

    /** Dòng dữ liệu: mã món|mã loại|tên loại|tên món|giá|thời gian|hình ảnh */
    static String dishRow(ResultSet rs) throws SQLException {
        return rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3) + "|" + rs.getString(4) + "|"
                + rs.getLong(5) + "|" + rs.getInt(6) + "|" + nz(rs.getString(7));
    }

    static String dishGet(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(DISH_SELECT + "WHERE d.id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? dishRow(rs) : null;
            }
        }
    }

    /** p = [lệnh, mã món, mã loại, tên món, hình ảnh, giá, thời gian phục vụ] */
    static String dishSave(String[] p, boolean add) throws SQLException {
        String id = p[1], catId = p[2], name = p[3], image = p[4];
        if (!id.matches(ID_REGEX)) return err("Mã món" + ID_MSG);
        if (catId.isEmpty()) return err("Chưa chọn loại món");
        if (!catId.matches(ID_REGEX)) return err("Mã loại món không hợp lệ");
        if (name.isEmpty() || name.length() > 100) return err("Tên món không được để trống và tối đa 100 ký tự");
        if (image.length() > 255) return err("Đường dẫn hình ảnh tối đa 255 ký tự");
        if (!p[5].matches(NUM_REGEX)) return err("Giá phải là số nguyên dương, không có dấu chấm/phẩy (VD: 50000)");
        long price = Long.parseLong(p[5]);
        if (price <= 0) return err("Giá phải lớn hơn 0");
        if (!p[6].matches(TIME_REGEX)) return err("Thời gian phục vụ phải là số nguyên (phút)");
        int time = Integer.parseInt(p[6]);
        if (time <= 0) return err("Thời gian phục vụ phải lớn hơn 0");

        try (Connection c = DBConnection.get()) {
            if (!exists(c, "SELECT COUNT(*) FROM category WHERE id=?", catId)) return err("Loại món không tồn tại");
            String before = dishGet(c, id);
            if (add && before != null) return err("Mã món đã tồn tại");
            if (!add && before == null) return err("Không tìm thấy món ăn: " + id);

            String sql = add ? "INSERT INTO dish(category_id,name,image,price,serve_time,id) VALUES(?,?,?,?,?,?)"
                             : "UPDATE dish SET category_id=?, name=?, image=?, price=?, serve_time=? WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, catId);
                ps.setString(2, name);
                ps.setString(3, image);
                ps.setLong(4, price);
                ps.setInt(5, time);
                ps.setString(6, id);
                ps.executeUpdate();
            }
            if (add) return ok("Thêm món ăn thành công: " + id + " - " + name);
            return ok("Sửa món ăn thành công: " + id) + "\n" + before + "\n" + dishGet(c, id);
        }
    }

    static String dishDelete(String id) throws SQLException {
        if (id.isEmpty()) return err("Chưa nhập mã món");
        try (Connection c = DBConnection.get()) {
            if (!exists(c, "SELECT COUNT(*) FROM dish WHERE id=?", id)) return err("Không tìm thấy món ăn");
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM dish WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        }
        return ok("Xóa món ăn thành công: " + id);
    }

    /** p = [lệnh, mã loại, từ khóa, giá từ, giá đến, thời gian tối đa]; trường rỗng = không lọc. */
    static String dishSearch(String[] p) throws SQLException {
        String catId = p[1], kw = p[2], min = p[3], max = p[4], maxTime = p[5];
        if (!min.isEmpty() && !min.matches(NUM_REGEX)) return err("Giá từ phải là số nguyên không âm");
        if (!max.isEmpty() && !max.matches(NUM_REGEX)) return err("Giá đến phải là số nguyên không âm");
        if (!min.isEmpty() && !max.isEmpty() && Long.parseLong(min) > Long.parseLong(max))
            return err("Giá từ không được lớn hơn giá đến");
        if (!maxTime.isEmpty() && !maxTime.matches(TIME_REGEX)) return err("Thời gian phục vụ phải là số nguyên (phút)");

        StringBuilder sql = new StringBuilder(DISH_SELECT + "WHERE (d.id LIKE ? OR d.name LIKE ?)");
        List<Object> params = new ArrayList<>();
        params.add(like(kw));
        params.add(like(kw));
        if (!catId.isEmpty())  { sql.append(" AND d.category_id=?"); params.add(catId); }
        if (!min.isEmpty())    { sql.append(" AND d.price>=?");      params.add(Long.parseLong(min)); }
        if (!max.isEmpty())    { sql.append(" AND d.price<=?");      params.add(Long.parseLong(max)); }
        if (!maxTime.isEmpty()) { sql.append(" AND d.serve_time<=?"); params.add(Integer.parseInt(maxTime)); }
        sql.append(" ORDER BY d.id");

        StringBuilder sb = new StringBuilder();
        int n = 0;
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append("\n").append(dishRow(rs));
                    n++;
                }
            }
        }
        return ok("Tìm thấy " + n + " món ăn") + sb;
    }
}
