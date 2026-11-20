package vn.edu.qltv;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Path2D;
import java.sql.*;
import java.time.LocalDate;

/** Ứng dụng desktop quản lý thư viện, dùng JDBC và SQL Server. */
public class QuanLyThuVienApp extends JFrame {
    private static final Color PAGE_BG = new Color(244, 247, 251);
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color NAV_BLUE = new Color(20, 89, 177);
    private static final Color TEXT_DARK = new Color(30, 48, 72);
    private static final Color TEXT_MUTED = new Color(102, 119, 141);
    private static final Color BORDER = new Color(220, 228, 238);
    private final JTable books = table();
    private final JTable readers = table();
    private final JTable employees = table();
    private final JTable categories = table();
    private final JTable publishers = table();
    private final JTable loans = table();
    private final JTable overdue = table();
    private final JTextField bookSearch = new JTextField(20);
    private final JTextField readerSearch = new JTextField(16);
    private final JTextField bookYearFilter = new JTextField(5);
    private final JComboBox<LookupOption> bookCategoryFilter = new JComboBox<>();
    private final JComboBox<LookupOption> bookPublisherFilter = new JComboBox<>();
    private final JComboBox<LookupOption> bookAuthorFilter = new JComboBox<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    public QuanLyThuVienApp() {
        super("Phần mềm quản lý thư viện");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        content.add(homePanel(), "home");
        content.add(bookPanel(), "books");
        content.add(readerPanel(), "readers");
        content.add(employeePanel(), "employees");
        content.add(catalogPanel(), "catalogs");
        content.add(loanPanel(), "loans");
        content.add(overduePanel(), "overdue");
        setJMenuBar(createNavigationBar());
        setContentPane(content);
        cardLayout.show(content, "home");
        refreshAll();
    }

    private JMenuBar createNavigationBar() {
        Color blue = new Color(20, 89, 177);
        JMenuBar bar = new JMenuBar();
        bar.setBackground(blue);
        bar.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        bar.setOpaque(true);

        JMenu system = styledMenu("Hệ thống", "settings", blue);
        addMenuItem(system, "Trang chủ", "home", "home");
        system.addSeparator();
        JMenuItem refresh = styledItem("Làm mới dữ liệu", "refresh");
        refresh.addActionListener(e -> refreshAll());
        system.add(refresh);
        JMenuItem exit = styledItem("Thoát", "exit");
        exit.addActionListener(e -> dispose());
        system.add(exit);

        JMenu manage = styledMenu("Quản lý", "manage", blue);
        addMenuItem(manage, "Quản lý sách", "books", "book");
        addMenuItem(manage, "Quản lý độc giả", "readers", "reader");
        addMenuItem(manage, "Quản lý nhân viên", "employees", "staff");
        addMenuItem(manage, "Thể loại và nhà xuất bản", "catalogs", "catalog");
        addMenuItem(manage, "Lập phiếu mượn / trả", "loans", "loan");

        JMenu search = styledMenu("Tra cứu", "search", blue);
        addMenuItem(search, "Tra cứu sách", "books", "book");
        addMenuItem(search, "Tra cứu độc giả", "readers", "reader");
        addMenuItem(search, "Sách quá hạn", "overdue", "overdue");

        JMenu help = styledMenu("Trợ giúp", "help", blue);
        JMenuItem guide = styledItem("Hướng dẫn sử dụng", "help");
        guide.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Chọn mục trong menu Quản lý để thêm, sửa và tra cứu dữ liệu.\n" +
                        "Trong màn hình Sách, dùng các bộ lọc ở phía trên bảng.\n" +
                        "Khi lập phiếu mượn, chọn sách theo mã và tên trong danh sách.",
                "Hướng dẫn sử dụng", JOptionPane.INFORMATION_MESSAGE));
        help.add(guide);
        JMenuItem about = styledItem("Giới thiệu", "info");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Phần mềm quản lý thư viện\nJava Swing · JDBC · Microsoft SQL Server",
                "Giới thiệu", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(system); bar.add(manage); bar.add(search); bar.add(help);
        return bar;
    }

    private JMenu styledMenu(String title, String icon, Color background) {
        JMenu menu = new JMenu(title);
        menu.setIcon(new VectorIcon(icon, 18, Color.WHITE));
        menu.setOpaque(true);
        menu.setBackground(background);
        menu.setForeground(Color.WHITE);
        menu.setFont(menu.getFont().deriveFont(Font.BOLD, 15f));
        menu.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        return menu;
    }

    private JMenuItem styledItem(String title, String icon) {
        JMenuItem item = new JMenuItem(title);
        item.setIcon(new VectorIcon(icon, 17, NAV_BLUE));
        item.setIconTextGap(10);
        item.setFont(item.getFont().deriveFont(14f));
        item.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 18));
        return item;
    }

    private void addMenuItem(JMenu menu, String label, String view, String icon) {
        JMenuItem item = styledItem(label, icon);
        item.addActionListener(e -> showView(view));
        menu.add(item);
    }

    private void showView(String view) {
        cardLayout.show(content, view);
        switch (view) {
            case "books" -> loadBooks(bookSearch.getText());
            case "readers" -> loadReaders(readerSearch.getText());
            case "employees" -> loadEmployees();
            case "catalogs" -> { loadCategories(); loadPublishers(); }
            case "loans" -> loadLoans();
            case "overdue" -> loadOverdue();
            default -> { }
        }
    }

    private JPanel homePanel() {
        JPanel home = new JPanel(new GridBagLayout());
        home.setBackground(new Color(239, 246, 255));
        JPanel welcome = new JPanel();
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(new VectorIcon("library", 104, NAV_BLUE));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("PHẦN MỀM QUẢN LÝ THƯ VIỆN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 31));
        title.setForeground(new Color(14, 53, 119));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel subtitle = new JLabel("Quản lý sách · độc giả · mượn và trả sách");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        subtitle.setForeground(new Color(75, 99, 130));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel titleBand = new JPanel(new BorderLayout());
        titleBand.setBackground(new Color(211, 231, 255));
        titleBand.setBorder(BorderFactory.createEmptyBorder(18, 34, 18, 34));
        titleBand.add(title, BorderLayout.CENTER);
        titleBand.setPreferredSize(new Dimension(820, 78));
        titleBand.setMaximumSize(new Dimension(820, 78));

        JPanel shortcuts = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        shortcuts.setOpaque(false);
        shortcuts.add(homeButton("Quản lý sách", "books"));
        shortcuts.add(homeButton("Quản lý độc giả", "readers"));
        shortcuts.add(homeButton("Lập phiếu mượn", "loans"));

        welcome.add(icon);
        welcome.add(Box.createVerticalStrut(14));
        welcome.add(titleBand);
        welcome.add(Box.createVerticalStrut(14));
        welcome.add(subtitle);
        welcome.add(Box.createVerticalStrut(28));
        welcome.add(shortcuts);
        home.add(welcome);
        return home;
    }

    private JButton homeButton(String label, String view) {
        JButton button = new JButton(label);
        String icon = switch (view) { case "books" -> "book"; case "readers" -> "reader"; default -> "loan"; };
        button.setIcon(new VectorIcon(icon, 19, NAV_BLUE));
        button.setIconTextGap(9);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setForeground(new Color(15, 66, 145));
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(173, 202, 237)),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        button.addActionListener(e -> showView(view));
        return button;
    }

    private JPanel bookPanel() {
        JPanel p = panel();
        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(pageHeader("Quản lý sách", "Thêm, cập nhật, xóa và tra cứu kho sách."));

        JPanel toolbar = controlBar();
        bookSearch.setPreferredSize(new Dimension(280, 36));
        styleInput(bookSearch);
        toolbar.add(new JLabel("Tên sách")); toolbar.add(bookSearch);
        JButton find = new JButton("Tìm");
        JButton all = new JButton("Xóa bộ lọc");
        JButton add = new JButton("Thêm sách");
        JButton edit = new JButton("Sửa sách đã chọn");
        JButton delete = new JButton("Xóa sách đã chọn");
        find.addActionListener(e -> applyBookFilters());
        all.addActionListener(e -> clearBookFilters());
        add.addActionListener(e -> addBook());
        edit.addActionListener(e -> editBook());
        delete.addActionListener(e -> deleteBook());
        styleButton(find, "primary"); styleButton(all, "neutral");
        styleButton(add, "primary"); styleButton(edit, "neutral"); styleButton(delete, "danger");
        toolbar.add(find); toolbar.add(all);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(add); toolbar.add(edit); toolbar.add(delete);

        bookCategoryFilter.setPreferredSize(new Dimension(190, 34));
        bookPublisherFilter.setPreferredSize(new Dimension(190, 34));
        bookAuthorFilter.setPreferredSize(new Dimension(190, 34));
        bookYearFilter.setPreferredSize(new Dimension(90, 34));
        styleLookupCombo(bookCategoryFilter);
        styleLookupCombo(bookPublisherFilter);
        styleLookupCombo(bookAuthorFilter);
        styleInput(bookYearFilter);
        JPanel filters = new JPanel(new BorderLayout(10, 0));
        filters.setBackground(PANEL_BG);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        JPanel filterFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
        filterFields.setOpaque(false);
        addFilter(filterFields, "Thể loại", bookCategoryFilter);
        addFilter(filterFields, "Năm xuất bản", bookYearFilter);
        addFilter(filterFields, "Nhà xuất bản", bookPublisherFilter);
        addFilter(filterFields, "Tác giả", bookAuthorFilter);
        filters.add(filterFields, BorderLayout.CENTER);
        JButton filter = new JButton("Áp dụng lọc");
        filter.setIcon(new VectorIcon("filter", 16, Color.WHITE));
        filter.setIconTextGap(7);
        filter.addActionListener(e -> applyBookFilters());
        styleButton(filter, "primary");
        JPanel filterAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 7));
        filterAction.setOpaque(false);
        filterAction.add(filter);
        filters.add(filterAction, BorderLayout.EAST);

        north.add(toolbar); north.add(Box.createVerticalStrut(8)); north.add(filters);
        p.add(north, BorderLayout.NORTH);
        p.add(tableScroll(books), BorderLayout.CENTER);
        return p;
    }

    private JPanel readerPanel() {
        JPanel p = panel();
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(pageHeader("Quản lý độc giả", "Thông tin thẻ, liên hệ và trạng thái của bạn đọc."));
        JPanel top = controlBar();
        JButton add = new JButton("Thêm độc giả");
        JButton edit = new JButton("Sửa độc giả đã chọn");
        JButton delete = new JButton("Xóa độc giả đã chọn");
        JButton refresh = new JButton("Làm mới");
        JButton find = new JButton("Tìm tên / SĐT / email");
        JButton all = new JButton("Xóa tìm kiếm");
        add.addActionListener(e -> saveReader(null));
        edit.addActionListener(e -> editReader());
        delete.addActionListener(e -> deleteReader());
        refresh.addActionListener(e -> loadReaders());
        find.addActionListener(e -> loadReaders(readerSearch.getText()));
        all.addActionListener(e -> { readerSearch.setText(""); loadReaders(""); });
        readerSearch.setPreferredSize(new Dimension(250, 36));
        top.add(new JLabel("Tìm độc giả")); top.add(readerSearch); top.add(find); top.add(all);
        top.add(Box.createHorizontalStrut(12));
        top.add(add); top.add(edit); top.add(delete); top.add(refresh);
        styleButton(add, "primary"); styleButton(edit, "neutral");
        styleButton(delete, "danger"); styleButton(refresh, "neutral");
        styleButton(find, "primary"); styleButton(all, "neutral");
        north.add(top);
        p.add(north, BorderLayout.NORTH);
        p.add(tableScroll(readers), BorderLayout.CENTER);
        return p;
    }

    private JPanel employeePanel() {
        JPanel p = panel();
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(pageHeader("Quản lý nhân viên", "Mã nhân viên do hệ thống tự cấp. Chức vụ được chọn theo vai trò thư viện."));
        JPanel top = controlBar();
        JButton add = new JButton("Thêm nhân viên");
        JButton edit = new JButton("Sửa nhân viên đã chọn");
        JButton delete = new JButton("Xóa nhân viên đã chọn");
        JButton refresh = new JButton("Làm mới");
        add.addActionListener(e -> addEmployee());
        edit.addActionListener(e -> editEmployee());
        delete.addActionListener(e -> deleteEmployee());
        refresh.addActionListener(e -> loadEmployees());
        top.add(add); top.add(edit); top.add(delete); top.add(refresh);
        styleButton(add, "primary"); styleButton(edit, "neutral");
        styleButton(delete, "danger"); styleButton(refresh, "neutral");
        north.add(top);
        p.add(north, BorderLayout.NORTH);
        p.add(tableScroll(employees), BorderLayout.CENTER);
        return p;
    }

    private JPanel catalogPanel() {
        JPanel p = panel();
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(pageHeader("Danh mục thư viện", "Quản lý thể loại và nhà xuất bản dùng trong thông tin sách."));
        JPanel categoryPane = new JPanel(new BorderLayout(6, 6));
        categoryPane.setBackground(PANEL_BG);
        JPanel categoryTop = controlBar();
        categoryTop.add(new JLabel("Thể loại · sắp theo mã tăng dần"));
        JButton addCategory = new JButton("Thêm thể loại");
        addCategory.addActionListener(e -> addCategory());
        styleButton(addCategory, "primary");
        categoryTop.add(addCategory);
        categoryPane.add(categoryTop, BorderLayout.NORTH);
        categoryPane.add(tableScroll(categories), BorderLayout.CENTER);

        JPanel publisherPane = new JPanel(new BorderLayout(6, 6));
        publisherPane.setBackground(PANEL_BG);
        JPanel publisherTop = controlBar();
        publisherTop.add(new JLabel("Nhà xuất bản · sắp theo mã tăng dần"));
        JButton addPublisher = new JButton("Thêm nhà xuất bản");
        addPublisher.addActionListener(e -> addPublisher());
        styleButton(addPublisher, "primary");
        publisherTop.add(addPublisher);
        publisherPane.add(publisherTop, BorderLayout.NORTH);
        publisherPane.add(tableScroll(publishers), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoryPane, publisherPane);
        split.setResizeWeight(0.5); split.setBorder(null); split.setOpaque(false);
        split.setDividerSize(10);
        p.add(north, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);
        JButton refresh = new JButton("Làm mới danh sách");
        refresh.addActionListener(e -> { loadCategories(); loadPublishers(); });
        styleButton(refresh, "neutral");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setOpaque(false); bottom.add(refresh);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel loanPanel() {
        JPanel p = panel();
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(pageHeader("Mượn và trả sách", "Lập phiếu mượn, theo dõi hạn trả và ghi nhận sách được hoàn trả."));
        JPanel top = controlBar();
        JButton borrow = new JButton("Lập phiếu mượn");
        JButton ret = new JButton("Trả sách đã chọn");
        JButton refresh = new JButton("Làm mới");
        borrow.addActionListener(e -> borrowBook());
        ret.addActionListener(e -> returnBook());
        refresh.addActionListener(e -> loadLoans());
        top.add(borrow); top.add(ret); top.add(refresh);
        styleButton(borrow, "primary"); styleButton(ret, "neutral"); styleButton(refresh, "neutral");
        north.add(top);
        p.add(north, BorderLayout.NORTH);
        p.add(tableScroll(loans), BorderLayout.CENTER);
        return p;
    }

    private JPanel overduePanel() {
        JPanel p = panel();
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(pageHeader("Sách quá hạn", "Danh sách các đầu sách chưa trả sau hạn trả."));
        JPanel top = controlBar();
        JButton refresh = new JButton("Làm mới");
        refresh.addActionListener(e -> loadOverdue());
        styleButton(refresh, "neutral"); top.add(refresh);
        north.add(top);
        p.add(north, BorderLayout.NORTH);
        p.add(tableScroll(overdue), BorderLayout.CENTER);
        return p;
    }

    private static JPanel panel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(PAGE_BG);
        p.setBorder(new EmptyBorder(18, 22, 20, 22));
        return p;
    }

    private static JPanel pageHeader(String title, String subtitle) {
        JPanel header = new JPanel(); header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 25)); heading.setForeground(TEXT_DARK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, heading.getPreferredSize().height));
        JLabel detail = new JLabel(subtitle);
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 14)); detail.setForeground(TEXT_MUTED);
        detail.setHorizontalAlignment(SwingConstants.CENTER);
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        detail.setMaximumSize(new Dimension(Integer.MAX_VALUE, detail.getPreferredSize().height));
        header.add(heading); header.add(Box.createVerticalStrut(4)); header.add(detail);
        header.setBorder(new EmptyBorder(0, 2, 12, 2));
        return header;
    }

    private static JPanel controlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return bar;
    }

    private static void addFilter(JPanel panel, String label, JComponent input) {
        JPanel group = new JPanel(); group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(label);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f)); title.setForeground(TEXT_MUTED);
        group.add(title); group.add(Box.createVerticalStrut(3)); group.add(input);
        panel.add(group);
    }

    private static void styleInput(JTextField input) {
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setForeground(TEXT_DARK);
        input.setBackground(Color.WHITE);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(5, 9, 5, 9)));
    }

    private static void styleLookupCombo(JComboBox<LookupOption> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXT_DARK);
        combo.setBackground(Color.WHITE);
        combo.setRenderer(new LookupComboRenderer());
        combo.setFocusable(false);
    }

    private static void styleChoiceCombo(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXT_DARK);
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
        combo.setFocusable(false);
        combo.setPreferredSize(new Dimension(340, 36));
    }

    private static JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(10, 14, 10, 14)));
        return form;
    }

    private static void addFormRow(JPanel form, int row, String caption, JComponent input) {
        JLabel label = new JLabel(caption);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_MUTED);
        label.setPreferredSize(new Dimension(158, 34));
        if (input instanceof JTextField textField) {
            styleInput(textField);
            textField.setPreferredSize(new Dimension(340, 36));
        } else if (input instanceof JComboBox<?> combo) {
            styleChoiceCombo(combo);
        }

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.LINE_START;
        left.insets = new Insets(5, 4, 5, 12);
        form.add(label, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.weightx = 1; right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(5, 0, 5, 4);
        form.add(input, right);
    }

    private static void styleButton(JButton button, String style) {
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12.5f));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(9, 13, 9, 13));
        switch (style) {
            case "primary" -> { button.setBackground(NAV_BLUE); button.setForeground(Color.WHITE); }
            case "danger" -> { button.setBackground(new Color(179, 55, 55)); button.setForeground(Color.WHITE); }
            default -> { button.setBackground(new Color(235, 241, 248)); button.setForeground(TEXT_DARK); }
        }
        button.setOpaque(true);
    }

    private static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(900, 500));
        return scroll;
    }

    private static JTable table() {
        JTable t = new JTable();
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(31);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setForeground(TEXT_DARK);
        t.setGridColor(new Color(232, 237, 244));
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(new Color(211, 230, 252));
        t.setSelectionForeground(TEXT_DARK);
        t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                if (!selected) cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(247, 249, 252));
                cell.setForeground(TEXT_DARK);
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return cell;
            }
        });
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setForeground(new Color(48, 69, 96));
        t.getTableHeader().setBackground(new Color(230, 239, 250));
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    private static void fill(JTable table, String sql, Object... params) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                String[] names = new String[md.getColumnCount()];
                for (int i = 0; i < names.length; i++) names[i] = md.getColumnLabel(i + 1);
                DefaultTableModel model = new DefaultTableModel(names, 0) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
                while (rs.next()) {
                    Object[] row = new Object[names.length];
                    for (int i = 0; i < row.length; i++) row[i] = rs.getObject(i + 1);
                    model.addRow(row);
                }
                table.setModel(model);
            }
        }
    }

    private void refreshBookFilters() {
        try {
            fillLookupFilter(bookCategoryFilter, "Tất cả thể loại", "SELECT matheloai,tentheloai FROM theloai ORDER BY matheloai ASC");
            fillLookupFilter(bookPublisherFilter, "Tất cả NXB", "SELECT manxb,tennxb FROM nhaxuatban ORDER BY manxb ASC");
            fillLookupFilter(bookAuthorFilter, "Tất cả tác giả", "SELECT matacgia,tentacgia FROM tacgia ORDER BY matacgia ASC");
        } catch (Exception ex) { error(ex); }
    }

    private static void fillLookupFilter(JComboBox<LookupOption> combo, String allLabel, String sql) throws SQLException {
        combo.removeAllItems();
        combo.addItem(new LookupOption(0, allLabel));
        for (LookupOption option : loadOptions(sql)) combo.addItem(option);
    }

    private void clearBookFilters() {
        bookSearch.setText(""); bookYearFilter.setText("");
        if (bookCategoryFilter.getItemCount() > 0) bookCategoryFilter.setSelectedIndex(0);
        if (bookPublisherFilter.getItemCount() > 0) bookPublisherFilter.setSelectedIndex(0);
        if (bookAuthorFilter.getItemCount() > 0) bookAuthorFilter.setSelectedIndex(0);
        applyBookFilters();
    }

    private void applyBookFilters() {
        int year = 0;
        try {
            if (!bookYearFilter.getText().isBlank()) {
                year = Integer.parseInt(bookYearFilter.getText().trim());
                if (year < 1) throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            error(new IllegalArgumentException("Năm xuất bản phải là số nguyên dương, ví dụ 2020."));
            return;
        }
        int categoryId = selectedLookupId(bookCategoryFilter);
        int publisherId = selectedLookupId(bookPublisherFilter);
        int authorId = selectedLookupId(bookAuthorFilter);
        int selectedYear = year;
        String search = bookSearch.getText().trim();
        String sql = "SELECT v.masach AS MaSach,v.tensach AS TenSach,v.tentheloai AS TheLoai,v.tennxb AS NhaXuatBan,v.namxuatban AS NamXuatBan,v.tacgia AS TacGia,v.soluongtong AS TongSo,v.soluongcon AS ConLai,s.isbn AS ISBN " +
                "FROM v_sachchitiet v JOIN sach s ON s.masach=v.masach " +
                "WHERE v.tensach LIKE ? AND (?=0 OR s.matheloai=?) AND (?=0 OR ISNULL(s.manxb,0)=?) " +
                "AND (?=0 OR s.namxuatban=?) AND (?=0 OR EXISTS(SELECT 1 FROM sach_tacgia st WHERE st.masach=s.masach AND st.matacgia=?)) ORDER BY s.masach ASC";
        runUi(() -> fill(books, sql, "%" + search + "%", categoryId, categoryId,
                publisherId, publisherId, selectedYear, selectedYear, authorId, authorId));
    }

    private static int selectedLookupId(JComboBox<LookupOption> combo) {
        LookupOption option = (LookupOption) combo.getSelectedItem();
        return option == null ? 0 : option.id;
    }

    private void loadBooks(String search) {
        bookSearch.setText(search == null ? "" : search);
        applyBookFilters();
    }

    private void loadReaders() {
        loadReaders("");
    }

    private void loadReaders(String search) {
        String query = search == null ? "" : search.trim();
        runUi(() -> fill(readers, "SELECT madocgia AS MaDocGia,hoten AS HoTen,ngaysinh AS NgaySinh,gioitinh AS GioiTinh,diachi AS DiaChi,sodienthoai AS DienThoai,email AS Email,ngaydangky AS NgayDangKy,ngayhethan AS NgayHetHan,trangthai AS TrangThai FROM docgia WHERE hoten LIKE ? OR sodienthoai LIKE ? OR email LIKE ? ORDER BY madocgia",
                "%" + query + "%", "%" + query + "%", "%" + query + "%"));
    }

    private void loadEmployees() {
        runUi(() -> fill(employees, "SELECT manhanvien AS MaNhanVien,hoten AS HoTen,chucvu AS ChucVu,sodienthoai AS DienThoai,email AS Email FROM nhanvien ORDER BY manhanvien"));
    }

    private void loadCategories() {
        runUi(() -> fill(categories, "SELECT matheloai AS MaTheLoai,tentheloai AS TenTheLoai FROM theloai ORDER BY matheloai ASC"));
    }

    private void loadPublishers() {
        runUi(() -> fill(publishers, "SELECT manxb AS MaNXB,tennxb AS TenNXB,diachi AS DiaChi FROM nhaxuatban ORDER BY manxb ASC"));
    }

    private void loadLoans() {
        runUi(() -> fill(loans, "SELECT pm.maphieu AS MaPhieu,ct.masach AS MaSach,dg.hoten AS DocGia,nv.manhanvien AS MaNhanVien,nv.hoten AS NhanVien,s.tensach AS TenSach,ct.soluong AS SoLuong,pm.ngaymuon AS NgayMuon,pm.hantra AS HanTra,ct.ngaytra AS NgayTra,ct.tienphat AS TienPhat FROM chitietmuon ct JOIN phieumuon pm ON pm.maphieu=ct.maphieu JOIN docgia dg ON dg.madocgia=pm.madocgia JOIN nhanvien nv ON nv.manhanvien=pm.manhanvien JOIN sach s ON s.masach=ct.masach WHERE ct.ngaytra IS NULL ORDER BY pm.hantra"));
    }

    private void loadOverdue() {
        runUi(() -> fill(overdue, "SELECT maphieu AS MaPhieu,hoten AS DocGia,sodienthoai AS DienThoai,tensach AS TenSach,hantra AS HanTra,songaytre AS SoNgayTre FROM v_sachquahan ORDER BY hantra"));
    }

    private void refreshAll() {
        try (Connection ignored = Database.get()) {
            // Kiểm tra một lần trước khi nạp nhiều bảng để tránh hiện nhiều hộp thoại lỗi.
        } catch (Exception ex) {
            error(ex);
            return;
        }
        refreshBookFilters();
        loadBooks(""); loadReaders(); loadEmployees(); loadCategories(); loadPublishers(); loadLoans(); loadOverdue();
    }

    private void runUi(SqlAction action) {
        try { action.run(); } catch (Exception ex) { error(ex); }
    }

    @FunctionalInterface private interface SqlAction { void run() throws Exception; }

    private void addBook() { saveBook(null); }

    private void editBook() {
        Integer id = selectedId(books, "Chọn sách cần sửa.");
        if (id != null) saveBook(id);
    }

    private void deleteBook() {
        Integer id = selectedId(books, "Chọn sách cần xóa.");
        if (id == null) return;
        String name = cell(books, selectedModelRow(books), 1);
        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa sách '" + name + "' (mã " + id + ")?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM sach WHERE masach=?")) {
            ps.setInt(1, id); ps.executeUpdate(); loadBooks(bookSearch.getText());
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 547) error(new IllegalStateException("Không thể xóa sách đã có lịch sử mượn."));
            else error(ex);
        }
    }

    private void saveBook(Integer bookId) {
        JTextField title = new JTextField();
        JTextField isbn = new JTextField();
        JComboBox<LookupOption> category = new JComboBox<>();
        JComboBox<LookupOption> publisher = new JComboBox<>();
        JList<LookupOption> authorList = new JList<>();
        authorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        authorList.setVisibleRowCount(5);
        authorList.setFixedCellHeight(34);
        authorList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        authorList.setSelectionBackground(new Color(218, 234, 253));
        authorList.setSelectionForeground(TEXT_DARK);
        authorList.setCellRenderer(new LookupListRenderer());
        authorList.setBorder(new EmptyBorder(4, 4, 4, 4));
        JButton addAuthor = new JButton("Thêm tác giả mới");
        addAuthor.addActionListener(e -> addAuthorToList(authorList));
        styleButton(addAuthor, "neutral");
        JPanel authorPanel = new JPanel(new BorderLayout(4, 4));
        authorPanel.setPreferredSize(new Dimension(340, 150));
        JScrollPane authorScroll = new JScrollPane(authorList);
        authorScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        authorScroll.getViewport().setBackground(Color.WHITE);
        authorPanel.add(authorScroll, BorderLayout.CENTER);
        JLabel authorHint = new JLabel("Giữ Ctrl để chọn nhiều tác giả");
        authorHint.setForeground(TEXT_MUTED);
        authorHint.setFont(authorHint.getFont().deriveFont(11f));
        authorPanel.add(authorHint, BorderLayout.NORTH);
        authorPanel.add(addAuthor, BorderLayout.SOUTH);
        JTextField year = new JTextField();
        JTextField total = new JTextField("1");
        for (JTextField input : new JTextField[]{title, isbn, year, total}) styleInput(input);
        title.setPreferredSize(new Dimension(340, 34));
        isbn.setPreferredSize(new Dimension(220, 34));
        year.setPreferredSize(new Dimension(150, 34));
        total.setPreferredSize(new Dimension(150, 34));
        int oldTotal = 0;
        int oldAvailable = 0;
        try {
            for (LookupOption option : loadOptions("SELECT matheloai,tentheloai FROM theloai ORDER BY matheloai ASC")) category.addItem(option);
            publisher.addItem(new LookupOption(0, "Không chọn nhà xuất bản"));
            for (LookupOption option : loadOptions("SELECT manxb,tennxb FROM nhaxuatban ORDER BY manxb ASC")) publisher.addItem(option);
            java.util.List<LookupOption> authorOptions = loadOptions("SELECT matacgia,tentacgia FROM tacgia ORDER BY matacgia ASC");
            authorList.setListData(authorOptions.toArray(new LookupOption[0]));
            if (bookId != null) {
                try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT isbn,tensach,matheloai,manxb,namxuatban,soluongtong,soluongcon FROM sach WHERE masach=?")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { error(new IllegalStateException("Không tìm thấy sách.")); return; }
                        isbn.setText(rs.getString("isbn"));
                        title.setText(rs.getString("tensach"));
                        selectLookup(category, rs.getInt("matheloai"));
                        int publisherId = rs.getInt("manxb");
                        selectLookup(publisher, rs.wasNull() ? 0 : publisherId);
                        int publicationYear = rs.getInt("namxuatban");
                        year.setText(rs.wasNull() ? "" : Integer.toString(publicationYear));
                        oldTotal = rs.getInt("soluongtong");
                        oldAvailable = rs.getInt("soluongcon");
                        total.setText(Integer.toString(oldTotal));
                    }
                }
                java.util.List<Integer> currentAuthorIds = new java.util.ArrayList<>();
                try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT matacgia FROM sach_tacgia WHERE masach=?")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) currentAuthorIds.add(rs.getInt(1)); }
                }
                java.util.List<Integer> selectedIndices = new java.util.ArrayList<>();
                for (int i = 0; i < authorList.getModel().getSize(); i++) {
                    if (currentAuthorIds.contains(authorList.getModel().getElementAt(i).id)) selectedIndices.add(i);
                }
                authorList.setSelectedIndices(selectedIndices.stream().mapToInt(Integer::intValue).toArray());
            }
        } catch (Exception ex) { error(ex); return; }
        styleLookupCombo(category);
        styleLookupCombo(publisher);
        if (category.getItemCount() == 0) {
            error(new IllegalStateException("Chưa có thể loại sách. Hãy thêm thể loại trong cơ sở dữ liệu trước."));
            return;
        }
        JPanel fields = formPanel();
        addFormRow(fields, 0, "Tên sách *", title);
        addFormRow(fields, 1, "ISBN (tùy chọn)", isbn);
        addFormRow(fields, 2, "Thể loại *", category);
        addFormRow(fields, 3, "Nhà xuất bản", publisher);
        addFormRow(fields, 4, "Tác giả", authorPanel);
        addFormRow(fields, 5, "Năm xuất bản", year);
        addFormRow(fields, 6, "Số lượng *", total);
        String dialogTitle = bookId == null ? "Thêm sách" : "Sửa sách - Mã: " + bookId;
        if (JOptionPane.showConfirmDialog(this, fields, dialogTitle, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (title.getText().isBlank()) { error(new IllegalArgumentException("Tên sách là bắt buộc.")); return; }
        if (isbn.getText().trim().length() > 20) {
            error(new IllegalArgumentException("ISBN tối đa 20 ký tự."));
            return;
        }
        try (Connection c = Database.get()) {
            LookupOption selectedCategory = (LookupOption) category.getSelectedItem();
            LookupOption selectedPublisher = (LookupOption) publisher.getSelectedItem();
            int n = Integer.parseInt(total.getText().trim());
            if (n < 0) throw new IllegalArgumentException("Số lượng không được âm.");
            if (!year.getText().isBlank() && Integer.parseInt(year.getText().trim()) < 1)
                throw new IllegalArgumentException("Năm xuất bản phải là số nguyên dương.");
            String sql = bookId == null
                    ? "INSERT INTO sach(isbn,tensach,matheloai,manxb,namxuatban,soluongtong,soluongcon) VALUES(?,?,?,?,?,?,?)"
                    : "UPDATE sach SET isbn=?,tensach=?,matheloai=?,manxb=?,namxuatban=?,soluongtong=?,soluongcon=? WHERE masach=?";
            c.setAutoCommit(false);
            int savedBookId = bookId == null ? 0 : bookId;
            try (PreparedStatement ps = bookId == null
                    ? c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                    : c.prepareStatement(sql)) {
                ps.setString(1, emptyNull(isbn.getText()));
                ps.setString(2, title.getText().trim()); ps.setInt(3, selectedCategory.id);
                if (selectedPublisher == null || selectedPublisher.id == 0) ps.setNull(4, Types.INTEGER);
                else ps.setInt(4, selectedPublisher.id);
                nullableInt(ps, 5, year.getText());
                int available = n;
                if (bookId != null) {
                    int borrowed = oldTotal - oldAvailable;
                    if (n < borrowed) throw new IllegalArgumentException("Tổng số lượng không thể nhỏ hơn số sách đang được mượn (" + borrowed + ").");
                    available = n - borrowed;
                }
                ps.setInt(6, n); ps.setInt(7, available);
                if (bookId != null) ps.setInt(8, bookId);
                ps.executeUpdate();
                if (bookId == null) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không lấy được mã sách mới.");
                        savedBookId = keys.getInt(1);
                    }
                }
            }
            try (PreparedStatement deleteAuthors = c.prepareStatement("DELETE FROM sach_tacgia WHERE masach=?")) {
                deleteAuthors.setInt(1, savedBookId); deleteAuthors.executeUpdate();
            }
            try (PreparedStatement insertAuthor = c.prepareStatement("INSERT INTO sach_tacgia(masach,matacgia) VALUES(?,?)")) {
                for (LookupOption author : authorList.getSelectedValuesList()) {
                    insertAuthor.setInt(1, savedBookId); insertAuthor.setInt(2, author.id); insertAuthor.addBatch();
                }
                insertAuthor.executeBatch();
            }
            c.commit();
            loadBooks(bookSearch.getText());
        } catch (Exception ex) { error(ex); }
    }

    private void addAuthorToList(JList<LookupOption> authorList) {
        String name = JOptionPane.showInputDialog(this, "Tên tác giả mới:", "Thêm tác giả", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO tacgia(tentacgia) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim()); ps.executeUpdate();
            int newId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được mã tác giả mới.");
                newId = keys.getInt(1);
            }
            java.util.List<Integer> selectedIds = new java.util.ArrayList<>();
            for (LookupOption selected : authorList.getSelectedValuesList()) selectedIds.add(selected.id);
            if (!selectedIds.contains(newId)) selectedIds.add(newId);
            java.util.List<LookupOption> options = loadOptions("SELECT matacgia,tentacgia FROM tacgia ORDER BY matacgia ASC");
            authorList.setListData(options.toArray(new LookupOption[0]));
            java.util.List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < authorList.getModel().getSize(); i++) {
                if (selectedIds.contains(authorList.getModel().getElementAt(i).id)) indices.add(i);
            }
            authorList.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
            refreshBookFilters();
        } catch (Exception ex) { error(ex); }
    }

    private static void selectLookup(JComboBox<LookupOption> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id == id) { combo.setSelectedIndex(i); return; }
        }
    }

    private void saveReader(Integer readerId) {
        JTextField name = new JTextField();
        JTextField birthDate = new JTextField();
        JComboBox<String> gender = new JComboBox<>(new String[]{"", "Nam", "Nu", "Khac"});
        JTextField address = new JTextField();
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JTextField expiry = new JTextField(LocalDate.now().plusYears(1).toString());

        if (readerId != null) {
            int row = selectedModelRow(readers);
            name.setText(cell(readers, row, 1));
            birthDate.setText(cell(readers, row, 2));
            gender.setSelectedItem(cell(readers, row, 3));
            address.setText(cell(readers, row, 4));
            phone.setText(cell(readers, row, 5));
            email.setText(cell(readers, row, 6));
            expiry.setText(cell(readers, row, 8));
        }

        styleChoiceCombo(gender);
        JPanel fields = formPanel();
        addFormRow(fields, 0, "Họ tên *", name);
        addFormRow(fields, 1, "Ngày sinh (yyyy-MM-dd)", birthDate);
        addFormRow(fields, 2, "Giới tính", gender);
        addFormRow(fields, 3, "Địa chỉ", address);
        addFormRow(fields, 4, "Số điện thoại", phone);
        addFormRow(fields, 5, "Email", email);
        addFormRow(fields, 6, "Hạn thẻ (yyyy-MM-dd) *", expiry);
        String title = readerId == null ? "Thêm độc giả" : "Sửa độc giả - Mã: " + readerId;
        if (JOptionPane.showConfirmDialog(this, fields, title, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (name.getText().isBlank()) { error(new IllegalArgumentException("Họ tên độc giả là bắt buộc.")); return; }
        try (Connection c = Database.get()) {
            String sql = readerId == null
                    ? "INSERT INTO docgia(hoten,ngaysinh,gioitinh,diachi,sodienthoai,email,ngayhethan) VALUES(?,?,?,?,?,?,?)"
                    : "UPDATE docgia SET hoten=?,ngaysinh=?,gioitinh=?,diachi=?,sodienthoai=?,email=?,ngayhethan=? WHERE madocgia=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name.getText().trim()); nullableDate(ps, 2, birthDate.getText());
                ps.setString(3, emptyNull((String) gender.getSelectedItem()));
                ps.setString(4, emptyNull(address.getText())); ps.setString(5, emptyNull(phone.getText()));
                ps.setString(6, emptyNull(email.getText()));
                ps.setDate(7, Date.valueOf(expiry.getText().trim()));
                if (readerId != null) ps.setInt(8, readerId);
                ps.executeUpdate();
            }
            loadReaders();
        } catch (Exception ex) { error(ex); }
    }

    private void editReader() {
        Integer id = selectedId(readers, "Chọn độc giả cần sửa.");
        if (id != null) saveReader(id);
    }

    private void deleteReader() {
        Integer id = selectedId(readers, "Chọn độc giả cần xóa.");
        if (id == null) return;
        String name = cell(readers, selectedModelRow(readers), 1);
        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa độc giả " + name + " (mã " + id + ")?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM docgia WHERE madocgia=?")) {
            ps.setInt(1, id); ps.executeUpdate(); loadReaders();
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 547) error(new IllegalStateException("Không thể xóa độc giả đã có phiếu mượn. Bạn có thể sửa thông tin thay vì xóa."));
            else error(ex);
        }
    }

    private void addEmployee() { saveEmployee(null); }

    private void editEmployee() {
        Integer id = selectedId(employees, "Chọn nhân viên cần sửa.");
        if (id != null) saveEmployee(id);
    }

    private void deleteEmployee() {
        Integer id = selectedId(employees, "Chọn nhân viên cần xóa.");
        if (id == null) return;
        String name = cell(employees, selectedModelRow(employees), 1);
        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa nhân viên " + name + " (mã " + id + ")?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM nhanvien WHERE manhanvien=?")) {
            ps.setInt(1, id); ps.executeUpdate(); loadEmployees();
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 547) error(new IllegalStateException("Không thể xóa nhân viên đã lập phiếu mượn."));
            else error(ex);
        }
    }

    private void saveEmployee(Integer employeeId) {
        JTextField name = new JTextField();
        JComboBox<String> position = new JComboBox<>(new String[]{
                "Thủ thư", "Quản lý thư viện", "Nhân viên mượn/trả", "Nhân viên nhập liệu"});
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        if (employeeId != null) {
            int row = selectedModelRow(employees);
            name.setText(cell(employees, row, 1));
            position.setSelectedItem(cell(employees, row, 2));
            phone.setText(cell(employees, row, 3));
            email.setText(cell(employees, row, 4));
        }
        JPanel fields = formPanel();
        addFormRow(fields, 0, "Họ tên *", name);
        addFormRow(fields, 1, "Chức vụ *", position);
        addFormRow(fields, 2, "Số điện thoại", phone);
        addFormRow(fields, 3, "Email", email);
        String title = employeeId == null ? "Thêm nhân viên" : "Sửa nhân viên - Mã: " + employeeId;
        if (JOptionPane.showConfirmDialog(this, fields, title, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (name.getText().isBlank()) { error(new IllegalArgumentException("Họ tên nhân viên là bắt buộc.")); return; }
        String sql = employeeId == null
                ? "INSERT INTO nhanvien(hoten,chucvu,sodienthoai,email) VALUES(?,?,?,?)"
                : "UPDATE nhanvien SET hoten=?,chucvu=?,sodienthoai=?,email=? WHERE manhanvien=?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name.getText().trim()); ps.setString(2, emptyNull((String) position.getSelectedItem()));
            ps.setString(3, emptyNull(phone.getText())); ps.setString(4, emptyNull(email.getText()));
            if (employeeId != null) ps.setInt(5, employeeId);
            ps.executeUpdate(); loadEmployees();
        } catch (Exception ex) { error(ex); }
    }

    private void addCategory() {
        JTextField name = new JTextField();
        JPanel fields = formPanel();
        addFormRow(fields, 0, "Tên thể loại *", name);
        if (JOptionPane.showConfirmDialog(this, fields,
                "Thêm thể loại", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (name.getText().isBlank()) { error(new IllegalArgumentException("Tên thể loại không được để trống.")); return; }
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO theloai(tentheloai) VALUES(?)")) {
            ps.setString(1, name.getText().trim());
            ps.executeUpdate();
            loadCategories();
            refreshBookFilters();
            JOptionPane.showMessageDialog(this, "Đã thêm thể loại. Mã được cấp tự động.");
        } catch (Exception ex) { error(ex); }
    }

    private void addPublisher() {
        JTextField name = new JTextField();
        JTextField address = new JTextField();
        JPanel fields = formPanel();
        addFormRow(fields, 0, "Tên nhà xuất bản *", name);
        addFormRow(fields, 1, "Địa chỉ", address);
        if (JOptionPane.showConfirmDialog(this, fields, "Thêm nhà xuất bản", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (name.getText().isBlank()) { error(new IllegalArgumentException("Tên nhà xuất bản không được để trống.")); return; }
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO nhaxuatban(tennxb,diachi) VALUES(?,?)")) {
            ps.setString(1, name.getText().trim());
            ps.setString(2, emptyNull(address.getText()));
            ps.executeUpdate();
            loadPublishers();
            refreshBookFilters();
            JOptionPane.showMessageDialog(this, "Đã thêm nhà xuất bản. Mã được cấp tự động.");
        } catch (Exception ex) { error(ex); }
    }

    private void borrowBook() {
        JComboBox<ReaderOption> reader = new JComboBox<>();
        JComboBox<EmployeeOption> staff = new JComboBox<>();
        JComboBox<BookOption> book = new JComboBox<>();
        JTextField days = new JTextField("14");
        reader.setPreferredSize(new Dimension(330, 32));
        staff.setPreferredSize(new Dimension(330, 32));
        book.setPreferredSize(new Dimension(330, 32));
        days.setColumns(8);
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT madocgia,hoten,sodienthoai FROM docgia WHERE trangthai=N'HoatDong' AND ngayhethan>=CAST(GETDATE() AS DATE) ORDER BY hoten, madocgia");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) reader.addItem(new ReaderOption(rs.getInt(1), rs.getString(2), rs.getString(3)));
        } catch (Exception ex) { error(ex); return; }
        if (reader.getItemCount() == 0) { error(new IllegalStateException("Chưa có độc giả đang hoạt động. Hãy thêm độc giả trước khi lập phiếu.")); return; }
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT manhanvien,hoten FROM nhanvien WHERE chucvu IN (N'Thủ thư',N'Quản lý thư viện',N'Nhân viên mượn/trả') ORDER BY manhanvien"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) staff.addItem(new EmployeeOption(rs.getInt(1), rs.getString(2)));
        } catch (Exception ex) { error(ex); return; }
        if (staff.getItemCount() == 0) { error(new IllegalStateException("Chưa có nhân viên. Hãy thêm nhân viên trong tab Nhân viên trước khi lập phiếu.")); return; }
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT masach,tensach,soluongcon FROM sach WHERE soluongcon>0 ORDER BY masach"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) book.addItem(new BookOption(rs.getInt(1), rs.getString(2), rs.getInt(3)));
        } catch (Exception ex) { error(ex); return; }
        if (book.getItemCount() == 0) { error(new IllegalStateException("Hiện không có sách còn trong kho để mượn.")); return; }
        JPanel fields = formPanel();
        addFormRow(fields, 0, "Độc giả *", reader);
        addFormRow(fields, 1, "Nhân viên lập phiếu *", staff);
        addFormRow(fields, 2, "Sách *", book);
        addFormRow(fields, 3, "Số ngày mượn *", days);
        if (JOptionPane.showConfirmDialog(this, fields, "Lập phiếu mượn", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try (Connection c = Database.get(); CallableStatement cs = c.prepareCall("{call sp_taophieumuon(?,?,?,?)}")) {
            int loanDays = Integer.parseInt(days.getText().trim());
            if (loanDays < 1) throw new IllegalArgumentException("Số ngày mượn phải lớn hơn 0.");
            EmployeeOption selectedStaff = (EmployeeOption) staff.getSelectedItem();
            ReaderOption selectedReader = (ReaderOption) reader.getSelectedItem();
            if (selectedReader == null) throw new IllegalArgumentException("Hãy chọn độc giả.");
            cs.setInt(1, selectedReader.id);
            cs.setInt(2, selectedStaff.id);
            BookOption selectedBook = (BookOption) book.getSelectedItem();
            cs.setInt(3, selectedBook.id);
            cs.setInt(4, loanDays); cs.execute();
            loadLoans(); loadBooks(""); loadOverdue();
        } catch (Exception ex) { error(ex); }
    }

    private void returnBook() {
        int row = loans.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn dòng sách cần trả."); return; }
        int model = loans.convertRowIndexToModel(row);
        int ticket = Integer.parseInt(loans.getModel().getValueAt(model, 0).toString());
        int book = Integer.parseInt(loans.getModel().getValueAt(model, 1).toString());
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("UPDATE chitietmuon SET ngaytra=CAST(GETDATE() AS DATE) WHERE maphieu=? AND masach=? AND ngaytra IS NULL")) {
            ps.setInt(1, ticket); ps.setInt(2, book);
            if (ps.executeUpdate() == 0) {
                loadLoans();
                JOptionPane.showMessageDialog(this, "Phiếu này đã được trả hoặc không còn trong danh sách.",
                        "Không thể ghi nhận trả sách", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            loadLoans(); loadOverdue(); loadBooks("");
            JOptionPane.showMessageDialog(this, "Đã ghi nhận trả sách. CSDL tự cập nhật số lượng và tiền phạt.");
        } catch (Exception ex) { error(ex); }
    }

    private static Integer selectedId(JTable table, String message) {
        if (table.getSelectedRow() < 0) { JOptionPane.showMessageDialog(table, message); return null; }
        return Integer.parseInt(cell(table, selectedModelRow(table), 0));
    }

    private static int selectedModelRow(JTable table) { return table.convertRowIndexToModel(table.getSelectedRow()); }
    private static String cell(JTable table, int row, int column) {
        Object value = table.getModel().getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private static void nullableInt(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, Integer.parseInt(value.trim()));
    }

    private static void nullableDate(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, Types.DATE);
        else ps.setDate(index, Date.valueOf(value.trim()));
    }

    private static java.util.List<LookupOption> loadOptions(String sql) throws SQLException {
        java.util.List<LookupOption> options = new java.util.ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) options.add(new LookupOption(rs.getInt(1), rs.getString(2)));
        }
        return options;
    }

    private static String emptyNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private void error(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        JOptionPane.showMessageDialog(this, message, "Có lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private static final class EmployeeOption {
        private final int id;
        private final String name;
        private EmployeeOption(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return "Mã NV: " + id + " - " + name; }
    }

    private static final class ReaderOption {
        private final int id;
        private final String name;
        private final String phone;
        private ReaderOption(int id, String name, String phone) {
            this.id = id; this.name = name; this.phone = phone;
        }
        @Override public String toString() {
            return "Mã ĐG: " + id + " - " + name + (phone == null || phone.isBlank() ? "" : " (" + phone + ")");
        }
    }

    private static final class BookOption {
        private final int id;
        private final String title;
        private final int available;
        private BookOption(int id, String title, int available) {
            this.id = id; this.title = title; this.available = available;
        }
        @Override public String toString() {
            return "Mã sách: " + id + " - " + title + " (còn " + available + ")";
        }
    }

    private static final class LookupOption {
        private final int id;
        private final String name;
        private LookupOption(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return id == 0 ? name : name + "  ·  Mã " + id; }
    }

    private static class LookupComboRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(7, 9, 7, 9));
            if (!selected) { label.setBackground(Color.WHITE); label.setForeground(TEXT_DARK); }
            return label;
        }
    }

    private static class LookupListRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            label.setText(value instanceof LookupOption option
                    ? option.name + "    ·    Mã " + option.id : String.valueOf(value));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(5, 8, 5, 8));
            if (!selected) {
                label.setBackground(index % 2 == 0 ? Color.WHITE : new Color(247, 249, 252));
                label.setForeground(TEXT_DARK);
            }
            return label;
        }
    }

    /** Icon vector nét mảnh, đồng bộ màu xanh cho toàn bộ thanh điều hướng. */
    private static final class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        private VectorIcon(String name, int size, Color color) {
            this.name = name; this.size = size; this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            double scale = size / 24.0;
            g.scale(scale, scale);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (name) {
                case "library", "book" -> drawBook(g);
                case "reader" -> drawReader(g);
                case "staff" -> drawStaff(g);
                case "catalog", "manage" -> drawCatalog(g);
                case "loan" -> drawLoan(g);
                case "search" -> drawSearch(g);
                case "overdue" -> drawClock(g);
                case "home" -> drawHome(g);
                case "refresh" -> drawRefresh(g);
                case "exit" -> drawExit(g);
                case "settings" -> drawSettings(g);
                case "help" -> drawHelp(g);
                case "info" -> drawInfo(g);
                case "filter" -> drawFilter(g);
                default -> drawBook(g);
            }
            g.dispose();
        }

        private static void drawBook(Graphics2D g) {
            g.drawRoundRect(3, 3, 18, 17, 3, 3);
            g.drawLine(12, 4, 12, 19);
            g.drawLine(6, 8, 9, 8); g.drawLine(15, 8, 18, 8);
            g.drawLine(6, 12, 9, 12); g.drawLine(15, 12, 18, 12);
            g.drawArc(4, 17, 16, 5, 0, 180);
        }

        private static void drawReader(Graphics2D g) {
            g.drawOval(9, 3, 6, 6);
            g.draw(new java.awt.geom.RoundRectangle2D.Double(4, 12, 16, 9, 7, 7));
            g.drawLine(8, 13, 8, 20); g.drawLine(16, 13, 16, 20);
        }

        private static void drawStaff(Graphics2D g) {
            g.drawOval(9, 2, 6, 6);
            g.draw(new java.awt.geom.RoundRectangle2D.Double(4, 11, 16, 10, 3, 3));
            g.drawLine(12, 12, 10, 16); g.drawLine(10, 16, 12, 19); g.drawLine(12, 19, 14, 16); g.drawLine(14, 16, 12, 12);
        }

        private static void drawCatalog(Graphics2D g) {
            g.drawRoundRect(3, 4, 8, 16, 2, 2); g.drawRoundRect(13, 4, 8, 16, 2, 2);
            g.drawLine(6, 8, 9, 8); g.drawLine(16, 8, 19, 8);
            g.drawLine(6, 12, 9, 12); g.drawLine(16, 12, 19, 12);
        }

        private static void drawLoan(Graphics2D g) {
            g.drawRoundRect(3, 4, 13, 16, 2, 2); g.drawLine(6, 9, 13, 9); g.drawLine(6, 13, 13, 13);
            g.drawLine(14, 17, 21, 17); g.drawLine(18, 14, 21, 17); g.drawLine(18, 20, 21, 17);
        }

        private static void drawSearch(Graphics2D g) {
            g.drawOval(3, 3, 13, 13); g.drawLine(13, 13, 21, 21);
        }

        private static void drawClock(Graphics2D g) {
            g.drawOval(3, 3, 18, 18); g.drawLine(12, 6, 12, 12); g.drawLine(12, 12, 16, 14);
        }

        private static void drawHome(Graphics2D g) {
            g.drawLine(3, 11, 12, 3); g.drawLine(12, 3, 21, 11);
            g.drawRoundRect(5, 10, 14, 11, 2, 2); g.drawRect(10, 15, 4, 6);
        }

        private static void drawRefresh(Graphics2D g) {
            g.drawArc(4, 4, 16, 16, 35, 255); g.drawLine(17, 3, 20, 7); g.drawLine(20, 7, 15, 8);
            g.drawLine(7, 17, 4, 21); g.drawLine(4, 21, 9, 20);
        }

        private static void drawExit(Graphics2D g) {
            g.drawRoundRect(3, 4, 10, 16, 2, 2); g.drawLine(10, 12, 21, 12);
            g.drawLine(17, 8, 21, 12); g.drawLine(17, 16, 21, 12);
        }

        private static void drawSettings(Graphics2D g) {
            g.drawOval(7, 7, 10, 10); g.drawOval(10, 10, 4, 4);
            g.drawLine(12, 2, 12, 6); g.drawLine(12, 18, 12, 22);
            g.drawLine(2, 12, 6, 12); g.drawLine(18, 12, 22, 12);
            g.drawLine(5, 5, 8, 8); g.drawLine(16, 16, 19, 19);
            g.drawLine(5, 19, 8, 16); g.drawLine(16, 8, 19, 5);
        }

        private static void drawHelp(Graphics2D g) {
            g.drawOval(3, 3, 18, 18);
            g.setFont(new Font("Segoe UI", Font.BOLD, 14)); g.drawString("?", 9, 17);
        }

        private static void drawInfo(Graphics2D g) {
            g.drawOval(3, 3, 18, 18); g.drawLine(12, 10, 12, 17); g.fillOval(11, 6, 2, 2);
        }

        private static void drawFilter(Graphics2D g) {
            Path2D funnel = new Path2D.Double();
            funnel.moveTo(3, 5); funnel.lineTo(21, 5); funnel.lineTo(14, 13);
            funnel.lineTo(14, 19); funnel.lineTo(10, 21); funnel.lineTo(10, 13); funnel.closePath();
            g.draw(funnel);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuanLyThuVienApp().setVisible(true));
    }
}
