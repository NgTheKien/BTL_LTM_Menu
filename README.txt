BTL LTM - Quan ly thuc don (UDP + MySQL/XAMPP)
Phan ca nhan: Sua loai mon, Sua mon, Hien thi ket qua sua.

0. Copy file mysql-connector-java-5.1.45.jar vao thu muc lib/ (lay tu repo mau hoac tai tren mang).
1. Mo XAMPP Control Panel -> Start "MySQL" (khong can Apache).
2. NetBeans: tao Java Project, copy thu muc src/menu vao, chuot phai Project > Properties > Libraries
   > Add JAR/Folder > lib/mysql-connector-java-5.1.45.jar
   (Hoac chay compile.bat roi run_server.bat, run_client.bat. Tren Mac: sh run_mac.sh compile|server|client)
3. Chuot phai src/menu/server/UDPServer.java > Run File  (chay truoc)
4. Chuot phai src/menu/client/UDPClient.java > Run File
   Server tu tao database "menu_db", cac bang va du lieu mau. Co the import db/menu_db.sql bang phpMyAdmin neu muon.
5. Neu MySQL co mat khau / cong khac: sua trong src/menu/server/DBConnection.java.
6. Sua MSSV va HOTEN o dau file src/menu/client/UDPClient.java.

GIAO THUC (chuoi UTF-8, cac truong cach nhau boi '|'):
  CAT_ADD|ma|ten|mota             CAT_UPDATE|ma|ten|mota
  CAT_DELETE|ma                   CAT_SEARCH|tukhoa
  DISH_ADD|ma|maloai|ten|anh|gia|thoigian
  DISH_UPDATE|ma|maloai|ten|anh|gia|thoigian
  DISH_DELETE|ma                  DISH_SEARCH|maloai|tukhoa|giatu|giaden|thoigiantoida
Tra loi: "OK\n<thong bao>\n<dong du lieu>..." hoac "ERR\n<thong bao loi>".
Voi CAT_UPDATE / DISH_UPDATE: dong 3 = ban ghi TRUOC khi sua, dong 4 = ban ghi SAU khi sua
-> client hien bang so sanh Truoc/Sau, to do cac truong thay doi (chuc nang "Hien thi ket qua sua").

KICH BAN SUA MON:
  Tab "Mon an" -> Tim & hien thi -> chon 1 dong (form tu dien) -> sua thong tin, chon loai mon tu danh sach
  -> "Sua mon" -> client kiem tra du lieu -> gui DISH_UPDATE -> server kiem tra lai, UPDATE CSDL, doc lai ban ghi
  -> tra ve truoc/sau -> client hien hop thoai ket qua sua + ghi vao o "Ket qua thuc hien" + lam moi bang.
