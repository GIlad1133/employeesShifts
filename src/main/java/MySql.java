import java.sql.*;
import java.util.Scanner;

public class MySql {

    static String jdbc = "jdbc:mysql://localhost:3306/nocode";
    static String user = "root";
    static String pass = "123";

    public static void main(String[] args) throws SQLException, ClassNotFoundException {


        System.out.println("Please enter your full name:");
        Scanner scan = new Scanner(System.in);
        String firstName = scan.next();
        System.out.println("Please enter your email adress:");
        String email = scan.next();

        createNewEmployee(firstName, email);
        signToShift("gilad@gmail.com", 2, 1);
        testSelect();
        removeEmployee("gilad@gmail.com");
        testSelect();
        signToShift("gilad@gmail.com", 2, 1);

        testSelect();

    }

    public static void testSelect() {

        try {
            Connection con = getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select * from employees");
            printResult(rs);
            con.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * @param name  - Employee name.
     * @param email - Employee email.
     */
    public static void createNewEmployee(String name, String email) {

        try {

            Connection con = getConnection();

            System.out.println("Going to create new record with Name " + name + " email adress: " + email);
            boolean exists = checkExist(email);
            if (!exists) {
                System.out.println("Going to insert to the table");
                PreparedStatement prepare = con.prepareStatement("INSERT INTO employees (name, email, is_active) VALUES (?, ?, ?)");
                prepare.setString(1, name);
                prepare.setString(2, email);
                prepare.setBoolean(3, true);
                prepare.execute();
                con.close();
                System.out.println("New data was inserted to employees, Name " + name + " email address: " + email);
            } else {
                System.out.println(email + "already exits");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * @param email - Employee email
     * @param week  - Number of the week
     * @param day   -  Number of the day
     * @throws SQLException           - SQL Exception
     * @throws ClassNotFoundException -
     */
    public static void signToShift(String email, int week, int day) throws SQLException, ClassNotFoundException {

        Connection con = getConnection();
        System.out.println("Got a request to sign in to a shift! \n Going to Check if the employee exists and active");
        boolean exists = checkExist(email);
        Boolean active = isActive(email);
        if (!active) {
            activateEmployee(email);
        }
        if (exists) {
            System.out.println("Going to search for employee ID");
            int id = getEmployeeID(email);

            boolean signEmployeeToShift = isShiftAvailable(id, week, day);
            if (signEmployeeToShift) {
                PreparedStatement prepare = con.prepareStatement("INSERT INTO shifts (employee_id, week, day) VALUES (?, ?, ?)");
                prepare.setInt(1, id);
                prepare.setInt(2, week);
                prepare.setInt(3, day);
                prepare.execute();
                System.out.println("Congrats! you are in the shifts, have a fucking nice dayyyyyyyyy");
            }
        }
    }

    /**
     *
     * @param email - Employee email
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private static void activateEmployee(String email) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        System.out.println("Going to ctivate employee");
        PreparedStatement prepare = con.prepareStatement("Update employees set is_active = 1 WHERE email = ?");
        prepare.setString(1, email);
        prepare.executeUpdate();
        System.out.println("Done activating employee from system");
    }

    /**
     * @param email - Employee email
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private static Boolean isActive(String email) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        System.out.println("Going to check if " + email + "is active");
        PreparedStatement prepare = con.prepareStatement("SELECT is_active FROM employees WHERE email = ?");
        prepare.setString(1, email);
        ResultSet rs = prepare.executeQuery();
        if (rs.next()) {
            int active = Integer.parseInt(rs.getString("is_active"));
            if (active == 1) {
                System.out.println(email + "is active");
                return true;
            }
        }
        System.out.println(email + "is not active");
        return false;

    }

    /**
     * @param email - Employee Email.
     * @return ID
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private static int getEmployeeID(String email) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        PreparedStatement prepare = con.prepareStatement("SELECT id FROM employees WHERE email = ?");
        prepare.setString(1, email);
        ResultSet rs = prepare.executeQuery();
        if (rs.next()) {
            int employeeId = Integer.parseInt(rs.getString("id"));
            System.out.println("Employee id is " + employeeId);
            return employeeId;
        }
        return 0;
    }

    /**
     * @param employeeId - Emplyee ID
     * @param week       - Number of the week
     * @param day        - Number of the day
     * @return boolean
     * @throws SQLException           .
     * @throws ClassNotFoundException .
     */
    private static boolean isShiftAvailable(int employeeId, int week, int day) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        PreparedStatement prepare = con.prepareStatement("SELECT * FROM shifts WHERE id = ? AND week = ? AND day = ?");
        prepare.setInt(1, employeeId);
        prepare.setInt(2, week);
        prepare.setInt(3, day);
        ResultSet rs = prepare.executeQuery();
        return !rs.next();
    }

    private static void removeEmployee(String email) throws SQLException, ClassNotFoundException {
        System.out.println("Starting removeEmployee with email " + email);
        Connection con = getConnection();
        int id = getEmployeeID(email);

        if (id == 0) {
            System.out.println("the employee does not exist in our system, sorry.");
            return;
        }
        if (checkEmployeeShifts(id)) {
            removeEmployeeFromShifts(id);
        } else {
            System.out.println("The employee does not have any shifts!");
        }
        deactivatingEmployee(id);
        System.out.println("Done removeEmployee with email " + email);
    }


    private static void removeEmployeeFromShifts(int employeeId) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        System.out.println("Going to Remove employee from shifts");
        PreparedStatement prepare = con.prepareStatement("Delete FROM shifts WHERE employee_id = ?");
        prepare.setInt(1, employeeId);
        prepare.executeUpdate();
        System.out.println("Done Remove employee from shifts");
    }

    private static void deactivatingEmployee(int employeeId) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        System.out.println("Going to deactivate employee");
        PreparedStatement prepare = con.prepareStatement("Update employees set is_active = 0 WHERE id = ?");
        prepare.setInt(1, employeeId);
        prepare.executeUpdate();
        System.out.println("Done Deactivating employee from system");
    }


    private static boolean checkEmployeeShifts(int employeeId) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        PreparedStatement prepare = con.prepareStatement("SELECT * FROM shifts WHERE employee_id = ?");
        prepare.setInt(1, employeeId);
        ResultSet rs = prepare.executeQuery();
        return rs.next();
    }

    /**
     * @return ?
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(
                jdbc, user, pass);
    }

    /**
     * @param email - Employee email
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private static boolean checkExist(String email) throws SQLException, ClassNotFoundException {
        Connection con = getConnection();
        PreparedStatement prepare = con.prepareStatement("SELECT * FROM employees WHERE email = ?");
        prepare.setString(1, email);
        System.out.println(prepare);
        ResultSet rs = prepare.executeQuery();
        System.out.println("Check if Email:" + email + " exists in system");
        return rs.isBeforeFirst();
    }


    private static void printResult(ResultSet rs) throws SQLException {
        while (rs.next())
            System.out.println(rs.getInt(1) + "  " + rs.getString(2) + "  " + rs.getString(3) + "  " + rs.getString(4));
    }

}
