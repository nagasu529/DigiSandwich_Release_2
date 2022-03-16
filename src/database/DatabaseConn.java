package database;

import java.sql.*;
import java.util.ArrayList;

/**
 *
 * @author Kitti
 */
public class DatabaseConn {

    //public ArrayList<String> quelyResult = new ArrayList<>();
    //public String Name, Ingrad1, Ingrad2, Ingrad3, Ingrad4, Ingrad5, Ingrad6;
    
    //Initial variable for selected database data.
    

    //Database connect for calculationg ET0
    private Connection connect(){
        //SQlite connietion string
        String url = "jdbc:sqlite:C:/Users/Krist/IdeaProjects/DigiSandwich_Release_2/src/database/DynamicMatchingDB.sqlite"; //My PC classpath
        //String url = "jdbc:sqlite:C:/Users/kitti/VSCode/DigiSandwich_Release_2/src/database/DynamicMatchingDB.sqlite"; //Office PC classpath
        //String url = "jdbc:sqlite:C:/Users/KChiewchanadmin/IdeaProjects/DigiSandwich_Release_2/src/database/DynamicMatchingDB.sqlite"; //Office NB classpath
        //String url = "jdbc:sqlite:/Users/nagasu/VSCodeProject/DigiSandwich_Release_2/src/database/DynamicMatchingDB.sqlite"; //MacBook VS Code
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        return conn;
    }
    /**
     * Select data from ET0 Cable
     *
     */
    public ArrayList<String> selectProduct(String productName) {
        ArrayList<String> quelyResult = new ArrayList<>();
        String sql  = String.format("SELECT * FROM Product WHERE Name='%s'",productName);
        //String sql = "SELECT * FROM Product WHERE name=''";
        try {
            Connection conn = this.connect();
            Statement statement = conn.createStatement();
            //PreparedStatement pstmt = conn.prepareStatement(sql);
            //pstmt.setString(1,productName);
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                quelyResult.add(resultSet.getString("ingradItem1"));
                quelyResult.add(resultSet.getString("ingradItem2"));
                quelyResult.add(resultSet.getString("ingradItem3"));
                quelyResult.add(resultSet.getString("ingradItem4"));
                quelyResult.add(resultSet.getString("ingradItem5"));
                quelyResult.add(resultSet.getString("ingradItem6"));
            }
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }catch (Exception e) {
            e.printStackTrace();
        }
        return quelyResult;
    }

    public double selectQuantity(String orderName, String ingradientName) {

        String sql  = String.format("SELECT %s FROM Quantity WHERE productName='%s'",ingradientName, orderName);
        double tempResut = 0.0;
        try {
            Connection conn = this.connect();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                tempResut = resultSet.getDouble(String.format("%s",ingradientName));
            }
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }catch (Exception e) {
            e.printStackTrace();
        }
        return tempResut;
    }
    //Selecting product data randomly from database.
    public String[] selectProductRandom() {
        //Query command.
       String productName = null;
       String grade = null;
       String sellingPrice = null;
       String productCost = null;
        String sql = String.format("SELECT * FROM Price ORDER BY RANDOM() LIMIT 1");
        try {
            Connection conn = this.connect();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                productName = resultSet.getString("productName");
                grade = resultSet.getString("grade");
                sellingPrice = String.valueOf(resultSet.getDouble("sellingPrice"));
                productCost = String.valueOf(resultSet.getDouble("productCost"));
            }
        }catch (SQLException e) {
            // TODO: handle exception
            System.err.format("SQL State: %s \n%s", e.getSQLState(), e.getMessage());
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        String[] resultArray = {productName, grade, sellingPrice, productCost};

        return resultArray;
    }


    //Order pricing for utility function calculation method.
    public double selectProductPrice(String productName, String orderGrade) {
        double tempResult = 0.0;
    	//Query command.
    	String sql = String.format("SELECT sellingPrice FROM Price WHERE productName='%s' AND grade='%s'", productName, orderGrade);
    	try {
    		Connection conn = this.connect();
    		Statement statement = conn.createStatement();
    		ResultSet resultSet = statement.executeQuery(sql);
    		while (resultSet.next()) {
                tempResult = resultSet.getDouble("sellingPrice");
			}
    	}catch (SQLException e) {
			// TODO: handle exception
    		System.err.format("SQL State: %s \n%s", e.getSQLState(), e.getMessage());
    	}catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
    	}
        return tempResult;
    }

    public double selectProductCost(String productName, String orderGrade) {
        double tempResult = 0.0;
    	//Query command.
    	String sql = String.format("SELECT productCost FROM price WHERE productName='%s' AND grade='%s'", productName, orderGrade);
    	try {
    		Connection conn = this.connect();
    		Statement statement = conn.createStatement();
    		ResultSet resultSet = statement.executeQuery(sql);
    		while (resultSet.next()) {
                tempResult = resultSet.getDouble("productCost");
			}
    	}catch (SQLException e) {
			// TODO: handle exception
    		System.err.format("SQL State: %s \n%s", e.getSQLState(), e.getMessage());
    	}catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
    	}
        return tempResult;
    }


    //Order analysis query which included get, update and delete rows.
    public void getDataByOrder(String ingradientName, String grade, int quantityTargetNeeded) {
        //Initial values for calculation method
        int tempResult = quantityTargetNeeded;
        //Database process.
        String sql = "SELECT quantity FROM ? WHERE grade=? BY orderID";
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            //set corresponding params.
            pstmt.setString(1,ingradientName);
            pstmt.setString(2,grade);

            //execute statement.
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while(resultSet.next()){
                int tempByRow = resultSet.getInt("quantity");
                tempResult = tempResult - tempByRow;
                if(tempResult > 0){
                    updateStockInRow(ingradientName,0);
                }else{
                    updateStockInRow(ingradientName,Math.abs(tempResult));
                    break;
                }
            }
            //delete row that contain 0 quantity.
            deleteRow(ingradientName,0);
        }catch(SQLException e){
            System.out.println(e);
        }
    }

    //Order analysis query which included get, update and delete rows.
    public int getIngradLifePeriod(String ingredientName) {
        //Initial values for calculation method
        int tempResult = 0;
        //Database process.
        String sql = String.format("SELECT StorageLife FROM IngredientInfo WHERE Name='%s'",ingredientName);
        try {
            Connection conn = this.connect();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                tempResult = resultSet.getInt("StorageLife");
            }
        }catch (SQLException e) {
            // TODO: handle exception
            System.err.format("SQL State: %s \n%s", e.getSQLState(), e.getMessage());
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        return tempResult;
    }


    //Insert data to database (Matching result).
    public void insertResult(String tableName, String matchingMethod, int participant, int numOfWinner, int numOfLost, int totalOrderReq, int totalOrderReject, double valueEarning){

        String sql = String.format("INSERT INTO %s (matchingMethod, participant, numOfWinner, numOfLost, totalOrderReq, totalOrderReject, valueEarning) VALUES (?,?,?,?,?,?,?)",tableName);
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,matchingMethod);
            pstmt.setInt(2,participant);
            pstmt.setInt(3, numOfWinner);
            pstmt.setInt(4,numOfLost);
            pstmt.setInt(5, totalOrderReq);
            pstmt.setInt(6,totalOrderReject);
            pstmt.setDouble(7,valueEarning);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    //Insert supplier stock to database.
    public void insertSupplier(double WhiteBread, double Ham, double Onion, double Pickle, double Tuna, double Spread){
        /***
        String sql = String.format("INSERT INTO IngredientResult (%s, WhiteBreadA, WhiteBreadB, WhiteBreadC, HamA, HamB, HamC, MatureCheddarMilkA, MatureCheddarMilkB, MatureCheddarMilkC," +
                "OnionA, OnionB, OnionC, PickleA, PickleB, PickleC, TunaA, TunaB, TunaC, SunflowerSpreadA, SunflowerSpreadB, SunflowerSpreadC, StockForEachIngrad) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",matchingMethod);
        ***/
        String sql = "INSERT INTO IngredientResult (WhiteBread, Ham, Onion, Pickle, Tuna, Spread) VALUES (?,?,?,?,?,?)";
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1,WhiteBread);
            pstmt.setDouble(2, Ham);
            pstmt.setDouble(3,Onion);
            pstmt.setDouble(4,Pickle);
            pstmt.setDouble(5,Tuna);
            pstmt.setDouble(6,Spread);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
    //Insert supplier stock to database.
    public void insertAllResult(double WhiteBread, double WhiteBread_after, double Ham, double Ham_after,double Onion, double Onion_after,double Pickle, double Pickle_after, double Tuna, double Tuna_after,
                                double Spread, double Spread_after){

        String sql = "INSERT INTO OverallResult (WhiteBread,WhiteBread_after,Ham,Ham_after,Onion,Onion_after,Pickle,Pickle_after,Tuna,Tuna_after,Spread,Spread_after) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1,WhiteBread);
            pstmt.setDouble(2,WhiteBread_after);
            pstmt.setDouble(3,Ham);
            pstmt.setDouble(4,Ham_after);
            pstmt.setDouble(5,Onion);
            pstmt.setDouble(6,Onion_after);
            pstmt.setDouble(7,Pickle);
            pstmt.setDouble(8,Pickle_after);
            pstmt.setDouble(9,Tuna);
            pstmt.setDouble(10,Tuna_after);
            pstmt.setDouble(11,Spread);
            pstmt.setDouble(12,Spread_after);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    //Insert data to database (Order transaction).
    public void orderTransaction(String tableName, int HamSandwich_order, int HamSandwich_accept, int HamSandwich_reject){

        String sql = String.format("INSERT INTO %s (HamSandwich_order, HamSandwich_accept, HamSandwich_reject) VALUES (?,?,?)",tableName);
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1,HamSandwich_order);
            pstmt.setInt(2,HamSandwich_accept);
            pstmt.setInt(3, HamSandwich_reject);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    //update statement
    public void updateStockInRow (String tableName, int latestStockNum){
        //String sql = String.format("UPDATE %s SET quantity=");
        String sql = "UPDATE ? SET quantity=?";
        try{
            Connection conn = this.connect();
            PreparedStatement psmt = conn.prepareStatement(sql);
            //set the corresponding params.
            psmt.setString(1,tableName);
            psmt.setInt(2,latestStockNum);
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public void insertWeeklyStock(double WhiteBread, double Ham, double Onion, double Pickle, double Tuna, double Spread){

        String sql = "INSERT INTO IngredientResult (WhiteBread, Ham, Onion, Pickle, Tuna, Spread) VALUES (?,?,?,?,?,?)";
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1,WhiteBread);
            pstmt.setDouble(2, Ham);
            pstmt.setDouble(3,Onion);
            pstmt.setDouble(4,Pickle);
            pstmt.setDouble(5,Tuna);
            pstmt.setDouble(6,Spread);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    //delete statement
    public void deleteRow(String tableName, int quantity){
        String sql = "DELETE * FROM ? WHERE quantity = ?";
        try{
            Connection conn = this.connect();
            PreparedStatement psmt = conn.prepareStatement(sql);
            //set the corresponding params.
            psmt.setString(1, tableName);
            psmt.setInt(2,quantity);
            //execute the delete statement.
            psmt.executeUpdate();

        } catch (SQLException e){
            System.out.println(e);
        }
    }
}