package menu.server;

import java.sql.*;

public class DBConnection {
    public static final String HOST = "localhost";
    public static final String PORT = "3306";
    public static final String DB   = "menu_db";
    public static final String USER = "root";
    public static final String PASS = "";
    static final String OPTS = "?useUnicode=true&characterEncoding=UTF-8&useSSL=false";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DB + OPTS, USER, PASS);
    }

    /** Tự tạo database, các bảng và dữ liệu mẫu nếu chưa có. */
    public static void init() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + OPTS, USER, PASS);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB + " CHARACTER SET utf8 COLLATE utf8_unicode_ci");
        }
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS category ("
                    + "id VARCHAR(20) PRIMARY KEY,"
                    + "name VARCHAR(100) NOT NULL UNIQUE,"
                    + "description VARCHAR(255)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dish ("
                    + "id VARCHAR(20) PRIMARY KEY,"
                    + "category_id VARCHAR(20) NOT NULL,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "image VARCHAR(255),"
                    + "price DECIMAL(15,0) NOT NULL,"
                    + "serve_time INT NOT NULL,"
                    + "FOREIGN KEY (category_id) REFERENCES category(id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");

            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM category")) {
                rs.next();
                if (rs.getInt(1) > 0) return;
            }
            s.executeUpdate("INSERT INTO category(id,name,description) VALUES "
                    + "('KV','Khai vị','Món ăn nhẹ trước bữa chính'),"
                    + "('MC','Món chính','Các món ăn chính trong bữa'),"
                    + "('TM','Tráng miệng','Món ngọt sau bữa ăn'),"
                    + "('DU','Đồ uống','Nước giải khát, trà, cà phê')");
            s.executeUpdate("INSERT INTO dish(id,category_id,name,image,price,serve_time) VALUES "
                    + "('KV01','KV','Gỏi cuốn tôm thịt','',45000,10),"
                    + "('KV02','KV','Súp cua','',40000,10),"
                    + "('MC01','MC','Phở bò tái','',65000,15),"
                    + "('MC02','MC','Cơm gà Hải Nam','',70000,20),"
                    + "('MC03','MC','Bún chả Hà Nội','',60000,15),"
                    + "('TM01','TM','Chè khúc bạch','',35000,5),"
                    + "('DU01','DU','Trà đào cam sả','',30000,5),"
                    + "('DU02','DU','Cà phê sữa đá','',25000,5)");
        }
    }
}
