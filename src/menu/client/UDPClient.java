package menu.client;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/** Client UDP (Swing) - Quản lý thực đơn: loại món & món ăn. */
public class UDPClient extends JFrame {
    static final String HOST = "localhost";
    static final int PORT = 9999;

    // >>> SỬA 2 DÒNG NÀY THÀNH THÔNG TIN CỦA BẠN <<<
    static final String MSSV  = "B24DTCN234";
    static final String HOTEN = "Nguyễn Thế Kiên";

    static final String[] CAT_COLS  = {"Mã loại", "Tên loại món", "Mô tả"};
    static final String[] DISH_COLS = {"Mã món", "Mã loại", "Loại món", "Tên món", "Giá", "Thời gian (phút)", "Hình ảnh"};

    // Loại món
    JTextField cId = new JTextField(), cName = new JTextField(), cDesc = new JTextField(), cSearch = new JTextField();
    DefaultTableModel cModel = new DefaultTableModel(CAT_COLS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    JTable cTable = new JTable(cModel);

    // Món ăn
    JComboBox<String> cbCat = new JComboBox<>();   // chọn loại món khi thêm/sửa
    JComboBox<String> sCat = new JComboBox<>();    // lọc theo loại khi tìm kiếm
    JTextField dId = new JTextField(), dName = new JTextField(), dImage = new JTextField(), dPrice = new JTextField(),
            dTime = new JTextField(), sKw = new JTextField(), sMin = new JTextField(), sMax = new JTextField(),
            sTime = new JTextField();
    JLabel imgPreview = new JLabel("(Không có ảnh)", SwingConstants.CENTER);
    DefaultTableModel dModel = new DefaultTableModel(DISH_COLS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    JTable dTable = new JTable(dModel);

    // Khu vực hiển thị kết quả
    JTextArea log = new JTextArea(6, 40);

    public UDPClient() {
        super("Quản lý thực đơn - UDP Client - " + MSSV + " - " + HOTEN);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Loại món", buildCatTab());
        tabs.addTab("Món ăn", buildDishTab());

        log.setEditable(false);
        JScrollPane lp = new JScrollPane(log);
        lp.setBorder(BorderFactory.createTitledBorder("Kết quả thực hiện"));

        JLabel info = new JLabel("MSSV: " + MSSV + "   -   Họ tên SV: " + HOTEN, SwingConstants.CENTER);
        info.setFont(info.getFont().deriveFont(Font.BOLD, 15f));
        info.setForeground(new Color(180, 0, 0));
        info.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        add(info, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(lp, BorderLayout.SOUTH);

        setSize(950, 700);
        setLocationRelativeTo(null);
        loadCats();
        searchDishes();
    }

    // ================= GIAO DIỆN =================
    JPanel buildCatTab() {
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Mã loại món:")); form.add(cId);
        form.add(new JLabel("Tên loại món:")); form.add(cName);
        form.add(new JLabel("Mô tả:")); form.add(cDesc);
        form.add(new JLabel("Từ khóa tìm kiếm (mã/tên):")); form.add(cSearch);

        JButton add = new JButton("Thêm"), upd = new JButton("Sửa"), del = new JButton("Xóa"), find = new JButton("Tìm kiếm");
        JPanel btns = new JPanel();
        btns.add(add); btns.add(upd); btns.add(del); btns.add(find);

        add.addActionListener(e -> doCmd(validCat(), catMsg("CAT_ADD"), this::afterCatChange));
        upd.addActionListener(e -> {
            if (!validCat()) return;
            List<String> r = doCmd(catMsg("CAT_UPDATE"), this::afterCatChange);
            if (r != null) showEditResult("Kết quả sửa loại món", CAT_COLS, r);
        });
        del.addActionListener(e -> doCmd(needId(cId, "Mã loại món"), "CAT_DELETE|" + cId.getText(), this::afterCatChange));
        find.addActionListener(e -> { if (noBad(cSearch, "Từ khóa")) fillTable(cModel, "CAT_SEARCH|" + cSearch.getText()); });

        cTable.getSelectionModel().addListSelectionListener(e -> {
            int r = cTable.getSelectedRow();
            if (r >= 0 && !e.getValueIsAdjusting()) {
                cId.setText(cModel.getValueAt(r, 0).toString());
                cName.setText(cModel.getValueAt(r, 1).toString());
                cDesc.setText(cModel.getValueAt(r, 2).toString());
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(cTable), BorderLayout.CENTER);
        return p;
    }

    JPanel buildDishTab() {
        JButton browse = new JButton("Chọn ảnh...");
        JPanel imgRow = new JPanel(new BorderLayout(5, 0));
        imgRow.add(dImage, BorderLayout.CENTER);
        imgRow.add(browse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Loại món:")); form.add(cbCat);
        form.add(new JLabel("Mã món:")); form.add(dId);
        form.add(new JLabel("Tên món:")); form.add(dName);
        form.add(new JLabel("Giá (VNĐ):")); form.add(dPrice);
        form.add(new JLabel("Thời gian phục vụ (phút):")); form.add(dTime);
        form.add(new JLabel("Hình ảnh:")); form.add(imgRow);

        JPanel search = new JPanel(new GridLayout(0, 2, 5, 5));
        search.setBorder(BorderFactory.createTitledBorder("Tìm kiếm"));
        search.add(new JLabel("Loại món:")); search.add(sCat);
        search.add(new JLabel("Từ khóa (mã/tên):")); search.add(sKw);
        search.add(new JLabel("Giá từ:")); search.add(sMin);
        search.add(new JLabel("Giá đến:")); search.add(sMax);
        search.add(new JLabel("Thời gian phục vụ tối đa (phút):")); search.add(sTime);

        imgPreview.setPreferredSize(new Dimension(170, 170));
        imgPreview.setBorder(BorderFactory.createTitledBorder("Ảnh món"));

        JButton add = new JButton("Thêm món"), upd = new JButton("Sửa món"), del = new JButton("Xóa món"), find = new JButton("Tìm & hiển thị");
        JPanel btns = new JPanel();
        btns.add(add); btns.add(upd); btns.add(del); btns.add(find);

        browse.addActionListener(e -> chooseImage());
        add.addActionListener(e -> doCmd(validDish(), dishMsg("DISH_ADD"), this::afterDishChange));
        upd.addActionListener(e -> {
            if (!validDish()) return;
            List<String> r = doCmd(dishMsg("DISH_UPDATE"), this::afterDishChange);
            if (r != null) showEditResult("Kết quả sửa món ăn", DISH_COLS, r);
        });
        del.addActionListener(e -> doCmd(needId(dId, "Mã món"), "DISH_DELETE|" + dId.getText(), this::afterDishChange));
        find.addActionListener(e -> searchDishes());

        dTable.getSelectionModel().addListSelectionListener(e -> {
            int r = dTable.getSelectedRow();
            if (r >= 0 && !e.getValueIsAdjusting()) {
                dId.setText(dModel.getValueAt(r, 0).toString());
                selectCat(cbCat, dModel.getValueAt(r, 1).toString());
                dName.setText(dModel.getValueAt(r, 3).toString());
                dPrice.setText(dModel.getValueAt(r, 4).toString());
                dTime.setText(dModel.getValueAt(r, 5).toString());
                dImage.setText(dModel.getValueAt(r, 6).toString());
                preview(dImage.getText());
            }
        });

        JPanel fields = new JPanel(new GridLayout(1, 2, 10, 0));
        fields.add(form);
        fields.add(search);
        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(fields, BorderLayout.CENTER);
        top.add(imgPreview, BorderLayout.EAST);
        top.add(btns, BorderLayout.SOUTH);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(dTable), BorderLayout.CENTER);
        return p;
    }

    // ================= KIỂM TRA DỮ LIỆU NHẬP =================
    static final String ID_REGEX = "[A-Za-z0-9_-]{1,20}";
    static final String NUM_REGEX = "[0-9]{1,13}";
    static final String TIME_REGEX = "[0-9]{1,4}";

    boolean fail(String msg, JComponent f) {
        log("[Lỗi] " + msg);
        f.requestFocus();
        return false;
    }

    /** Ký tự '|' và ký tự điều khiển (xuống dòng, tab...) làm hỏng giao thức. */
    boolean noBad(JTextField f, String label) {
        if (f.getText().matches("(?s).*[|\\p{Cntrl}].*"))
            return fail(label + " không được chứa ký tự '|' hoặc ký tự xuống dòng", f);
        return true;
    }

    boolean needText(JTextField f, String label, int max) {
        String v = f.getText().trim();
        if (v.isEmpty()) return fail(label + " không được để trống", f);
        if (v.length() > max) return fail(label + " tối đa " + max + " ký tự", f);
        return noBad(f, label);
    }

    boolean needId(JTextField f, String label) {
        String v = f.getText().trim();
        if (v.isEmpty()) return fail("Chưa nhập " + label.toLowerCase(), f);
        if (!v.matches(ID_REGEX))
            return fail(label + " chỉ gồm chữ không dấu, số, '_' hoặc '-' (tối đa 20 ký tự, không có khoảng trắng)", f);
        return true;
    }

    boolean validCat() {
        if (!needId(cId, "Mã loại món")) return false;
        if (!needText(cName, "Tên loại món", 100)) return false;
        if (cDesc.getText().trim().length() > 255) return fail("Mô tả tối đa 255 ký tự", cDesc);
        return noBad(cDesc, "Mô tả");
    }

    boolean validDish() {
        if (selectedCatId(cbCat).isEmpty()) return fail("Hãy chọn loại món từ danh sách", cbCat);
        if (!needId(dId, "Mã món")) return false;
        if (!needText(dName, "Tên món", 100)) return false;
        String p = dPrice.getText().trim();
        if (!p.matches(NUM_REGEX) || Long.parseLong(p) <= 0)
            return fail("Giá phải là số nguyên dương, không có dấu chấm/phẩy (VD: 50000)", dPrice);
        String t = dTime.getText().trim();
        if (!t.matches(TIME_REGEX) || Integer.parseInt(t) <= 0)
            return fail("Thời gian phục vụ phải là số nguyên dương (phút)", dTime);
        if (dImage.getText().trim().length() > 255) return fail("Đường dẫn hình ảnh tối đa 255 ký tự", dImage);
        return noBad(dImage, "Hình ảnh");
    }

    boolean validSearch() {
        if (!noBad(sKw, "Từ khóa")) return false;
        String a = sMin.getText().trim(), b = sMax.getText().trim(), t = sTime.getText().trim();
        if (!a.isEmpty() && !a.matches(NUM_REGEX)) return fail("Giá từ phải là số nguyên không âm", sMin);
        if (!b.isEmpty() && !b.matches(NUM_REGEX)) return fail("Giá đến phải là số nguyên không âm", sMax);
        if (!a.isEmpty() && !b.isEmpty() && Long.parseLong(a) > Long.parseLong(b))
            return fail("Giá từ không được lớn hơn giá đến", sMin);
        if (!t.isEmpty() && !t.matches(TIME_REGEX)) return fail("Thời gian phục vụ phải là số nguyên (phút)", sTime);
        return true;
    }

    /** Chỉ gửi lệnh khi dữ liệu hợp lệ. */
    void doCmd(boolean valid, String msg, Runnable onOk) {
        if (valid) doCmd(msg, onOk);
    }

    // ================= XÓA TRẮNG Ô NHẬP SAU KHI THÀNH CÔNG =================
    void afterCatChange() {
        loadCats();
        searchDishes();   // tên loại món hiển thị trong bảng món ăn có thể đã đổi
        cId.setText(""); cName.setText(""); cDesc.setText("");
        cId.requestFocus();
    }

    void afterDishChange() {
        searchDishes();
        dId.setText(""); dName.setText(""); dPrice.setText(""); dTime.setText(""); dImage.setText("");
        preview("");
        dId.requestFocus();
    }

    // ================= XỬ LÝ =================
    String selectedCatId(JComboBox<String> cb) {
        String s = (String) cb.getSelectedItem();
        if (s == null || s.startsWith("(Tất cả)")) return "";
        return s.substring(0, s.indexOf(" - "));
    }

    void selectCat(JComboBox<String> cb, String catId) {
        for (int i = 0; i < cb.getItemCount(); i++)
            if (cb.getItemAt(i).startsWith(catId + " - ")) cb.setSelectedIndex(i);
    }

    String catMsg(String cmd) {
        return cmd + "|" + cId.getText() + "|" + cName.getText() + "|" + cDesc.getText();
    }

    String dishMsg(String cmd) {
        return cmd + "|" + dId.getText() + "|" + selectedCatId(cbCat) + "|" + dName.getText() + "|"
                + dImage.getText() + "|" + dPrice.getText() + "|" + dTime.getText();
    }

    void searchDishes() {
        if (!validSearch()) return;
        fillTable(dModel, "DISH_SEARCH|" + selectedCatId(sCat) + "|" + sKw.getText() + "|" + sMin.getText() + "|"
                + sMax.getText() + "|" + sTime.getText());
    }

    /** Tải danh sách loại món vào bảng và 2 combobox. */
    void loadCats() {
        List<String> r = send("CAT_SEARCH|");
        if (r == null || !r.get(0).equals("OK")) return;
        cModel.setRowCount(0);
        String oldForm = (String) cbCat.getSelectedItem(), oldSearch = (String) sCat.getSelectedItem();
        cbCat.removeAllItems();
        sCat.removeAllItems();
        sCat.addItem("(Tất cả)");
        for (int i = 2; i < r.size(); i++) {
            if (r.get(i).isEmpty()) continue;
            String[] f = r.get(i).split("\\|", -1);
            cModel.addRow(f);
            cbCat.addItem(f[0] + " - " + f[1]);
            sCat.addItem(f[0] + " - " + f[1]);
        }
        if (oldForm != null) selectCat(cbCat, oldForm.substring(0, oldForm.indexOf(" - ")));
        if (oldSearch != null && !oldSearch.startsWith("(Tất cả)")) selectCat(sCat, oldSearch.substring(0, oldSearch.indexOf(" - ")));
    }

    /** Gửi lệnh thêm/sửa/xóa, hiển thị kết quả; nếu thành công thì chạy onOk và trả về phản hồi. */
    List<String> doCmd(String msg, Runnable onOk) {
        List<String> r = send(msg);
        if (r == null) return null;
        boolean ok = r.get(0).equals("OK");
        log((ok ? "[Thành công] " : "[Lỗi] ") + r.get(1));
        if (!ok) return null;
        if (onOk != null) onOk.run();
        return r;
    }

    /** Gửi lệnh tìm kiếm và đổ dữ liệu vào bảng. */
    void fillTable(DefaultTableModel m, String msg) {
        List<String> r = send(msg);
        if (r == null) return;
        boolean ok = r.get(0).equals("OK");
        log((ok ? "[Thành công] " : "[Lỗi] ") + r.get(1));
        if (!ok) return;
        m.setRowCount(0);
        for (int i = 2; i < r.size(); i++)
            if (!r.get(i).isEmpty()) m.addRow(r.get(i).split("\\|", -1));
    }

    // ================= HIỂN THỊ KẾT QUẢ SỬA =================
    /** r = [OK, thông báo, bản ghi trước khi sửa, bản ghi sau khi sửa]: hiện bảng so sánh, tô đỏ trường thay đổi. */
    void showEditResult(String title, String[] cols, List<String> r) {
        if (r.size() < 4) return;
        String[] before = r.get(2).split("\\|", -1), after = r.get(3).split("\\|", -1);

        DefaultTableModel m = new DefaultTableModel(new String[]{"Thuộc tính", "Trước khi sửa", "Sau khi sửa"}, 0) {
            public boolean isCellEditable(int row, int c) { return false; }
        };
        boolean[] changed = new boolean[cols.length];
        int n = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            m.addRow(new Object[]{cols[i], before[i], after[i]});
            changed[i] = !before[i].equals(after[i]);
            if (changed[i]) {
                n++;
                sb.append("\n    ").append(cols[i]).append(": \"").append(before[i]).append("\" -> \"").append(after[i]).append("\"");
            }
        }
        log("    Số trường thay đổi: " + n + sb);

        JTable t = new JTable(m);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tb, v, sel, foc, row, col);
                if (!sel) {
                    c.setForeground(changed[row] ? new Color(200, 0, 0) : Color.BLACK);
                    c.setFont(c.getFont().deriveFont(changed[row] ? Font.BOLD : Font.PLAIN));
                }
                return c;
            }
        });
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new Dimension(560, 40 + 18 * cols.length));
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(new JLabel(r.get(1) + "  -  Số trường thay đổi: " + n + " (tô đỏ)"), BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, p, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= HÌNH ẢNH =================
    void chooseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Ảnh (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dImage.setText(fc.getSelectedFile().getAbsolutePath());
            preview(dImage.getText());
        }
    }

    /** Ảnh chỉ lưu đường dẫn (không gửi byte ảnh qua UDP); xem trước nếu file có trên máy client. */
    void preview(String path) {
        if (!path.isEmpty() && new File(path).isFile()) {
            Image img = new ImageIcon(path).getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            imgPreview.setIcon(new ImageIcon(img));
            imgPreview.setText("");
        } else {
            imgPreview.setIcon(null);
            imgPreview.setText("(Không có ảnh)");
        }
    }

    void log(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    /** Gửi 1 datagram UDP và nhận phản hồi. */
    List<String> send(String msg) {
        try (DatagramSocket s = new DatagramSocket()) {
            s.setSoTimeout(3000);
            byte[] b = msg.getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(b, b.length, InetAddress.getByName(HOST), PORT));
            byte[] buf = new byte[65000];
            DatagramPacket in = new DatagramPacket(buf, buf.length);
            s.receive(in);
            String res = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
            return Arrays.asList(res.split("\n"));
        } catch (SocketTimeoutException e) {
            log("[Lỗi] Server không phản hồi (hết thời gian chờ). Server đã chạy chưa?");
        } catch (Exception e) {
            log("[Lỗi] " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPClient().setVisible(true));
    }
}
