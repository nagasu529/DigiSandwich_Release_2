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
        //String url = "jdbc:sqlite:C:/Users/kitti/VSCode/DynamicMatching-Java/src/database/DynamicMatchingDB.sqlite"; //Office PC classpath
        //String url = "jdbc:sqlite:C:/Users/KChiewchanadmin/IdeaProjects/DynamicMatching-Java/src/database/DynamicMatchingDB.sqlite"; //Office NB classpath
        //String url = "jdbc:sqlite:/Users/nagasu/VSCodeProject/DynamicMatching-Java/src/database/DynamicMatchingDB.sqlite"; //MacBook VS Code
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
        String sql = String.format("SELECT test FROM IngredientInfo WHERE Name='%s'",ingredientName);
        try {
            Connection conn = this.connect();
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                tempResult = resultSet.getInt("test");
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
    public void insertSupplier(double WhiteBreadA, double WhiteBreadB, double WhiteBreadC, double HamA, double HamB, double HamC, double MatureCheddarMilkA,
                               double MatureCheddarMilkB, double MatureCheddarMilkC, double OnionA, double OnionB, double OnionC, double PickleA, double PickleB, double PickleC, double TunaA, double TunaB, double TunaC,
                               double SunflowerSpreadA, double SunflowerSpreadB, double SunflowerSpreadC){
        /***
        String sql = String.format("INSERT INTO IngredientResult (%s, WhiteBreadA, WhiteBreadB, WhiteBreadC, HamA, HamB, HamC, MatureCheddarMilkA, MatureCheddarMilkB, MatureCheddarMilkC," +
                "OnionA, OnionB, OnionC, PickleA, PickleB, PickleC, TunaA, TunaB, TunaC, SunflowerSpreadA, SunflowerSpreadB, SunflowerSpreadC, StockForEachIngrad) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",matchingMethod);
        ***/
        String sql = "INSERT INTO IngredientResult (WhiteBreadA, WhiteBreadB, WhiteBreadC, HamA, HamB, HamC, MatureCheddarMilkA, MatureCheddarMilkB, MatureCheddarMilkC," +
                "OnionA, OnionB, OnionC, PickleA, PickleB, PickleC, TunaA, TunaB, TunaC, SunflowerSpreadA, SunflowerSpreadB, SunflowerSpreadC) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
         try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1,WhiteBreadA);
            pstmt.setDouble(2, WhiteBreadB);
            pstmt.setDouble(3,WhiteBreadC);
            pstmt.setDouble(4,HamA);
            pstmt.setDouble(5,HamB);
            pstmt.setDouble(6,HamC);
            pstmt.setDouble(7,MatureCheddarMilkA);
            pstmt.setDouble(8,MatureCheddarMilkB);
            pstmt.setDouble(9,MatureCheddarMilkC);
            pstmt.setDouble(10,OnionA);
            pstmt.setDouble(11,OnionB);
            pstmt.setDouble(12,OnionC);
            pstmt.setDouble(13,PickleA);
            pstmt.setDouble(14,PickleB);
            pstmt.setDouble(15,PickleC);
            pstmt.setDouble(16,TunaA);
            pstmt.setDouble(17,TunaB);
            pstmt.setDouble(18,TunaC);
            pstmt.setDouble(19,SunflowerSpreadA);
            pstmt.setDouble(20,SunflowerSpreadB);
            pstmt.setDouble(21,SunflowerSpreadC);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
    //Insert supplier stock to database.
    public void insertAllResult(double WhiteBreadA, double WhiteBreadA_after, double WhiteBreadB, double WhiteBreadB_after,double WhiteBreadC, double WhiteBreadC_after,double HamA, double HamA_after, double HamB, double HamB_after,
                                double HamC, double HamC_after, double MatureCheddarMilkA, double MatureCheddarMilkA_after, double MatureCheddarMilkB, double MatureCheddarMilkB_after, double MatureCheddarMilkC, double MatureCheddarMilkC_after,
                                double OnionA,double OnionA_after, double OnionB, double OnionB_after, double OnionC, double OnionC_after,double PickleA, double PickleA_after, double PickleB, double PickleB_after, double PickleC, double PickleC_after,
                                double TunaA, double TunaA_after, double TunaB, double TunaB_after, double TunaC, double TunaC_after, double SunflowerSpreadA, double SunflowerSpreadA_after, double SunflowerSpreadB, double SunflowerSpreadB_after,
                                double SunflowerSpreadC, double SunflowerSpreadC_after){

        String sql = "INSERT INTO OverallResult (WhiteBreadA, WhiteBreadA_after, WhiteBreadB, WhiteBreadB_after, WhiteBreadC, WhiteBreadC_after, HamA, HamA_after, HamB, HamB_after, HamC, HamC_after, MatureCheddarMilkA, MatureCheddarMilkA_after,  MatureCheddarMilkB, MatureCheddarMilkB_after," +
                "MatureCheddarMilkC, MatureCheddarMilkC_after, OnionA, OnionA_after, OnionB, OnionB_after, OnionC, OnionC_after, PickleA, PickleA_after, PickleB, PickleB_after, PickleC, PickleC_after, " +
                "TunaA, TunaA_after, TunaB, TunaB_after, TunaC, TunaC_after, SunflowerSpreadA, SunflowerSpreadA_after, SunflowerSpreadB, SunflowerSpreadB_after, SunflowerSpreadC, SunflowerSpreadC_after) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1,WhiteBreadA);
            pstmt.setDouble(2,WhiteBreadA_after);
            pstmt.setDouble(3,WhiteBreadB);
            pstmt.setDouble(4,WhiteBreadB_after);
            pstmt.setDouble(5,WhiteBreadC);
            pstmt.setDouble(6,WhiteBreadC_after);
            pstmt.setDouble(7,HamA);
            pstmt.setDouble(8,HamA_after);
            pstmt.setDouble(9,HamB);
            pstmt.setDouble(10,HamB_after);
            pstmt.setDouble(11,HamC);
            pstmt.setDouble(12,HamC_after);
            pstmt.setDouble(13,MatureCheddarMilkA);
            pstmt.setDouble(14,MatureCheddarMilkA_after);
            pstmt.setDouble(15,MatureCheddarMilkB);
            pstmt.setDouble(16,MatureCheddarMilkB_after);
            pstmt.setDouble(17,MatureCheddarMilkC);
            pstmt.setDouble(18,MatureCheddarMilkC_after);
            pstmt.setDouble(19,OnionA);
            pstmt.setDouble(20,OnionA_after);
            pstmt.setDouble(21,OnionB);
            pstmt.setDouble(22,OnionB_after);
            pstmt.setDouble(23,OnionC);
            pstmt.setDouble(24,OnionC_after);
            pstmt.setDouble(25,PickleA);
            pstmt.setDouble(26,PickleA_after);
            pstmt.setDouble(27,PickleB);
            pstmt.setDouble(28,PickleB_after);
            pstmt.setDouble(29,PickleC);
            pstmt.setDouble(30,PickleC_after);
            pstmt.setDouble(31,TunaA);
            pstmt.setDouble(32,TunaA_after);
            pstmt.setDouble(33,TunaB);
            pstmt.setDouble(34,TunaB_after);
            pstmt.setDouble(35,TunaC);
            pstmt.setDouble(36,TunaC_after);
            pstmt.setDouble(37,SunflowerSpreadA);
            pstmt.setDouble(38,SunflowerSpreadA_after);
            pstmt.setDouble(39,SunflowerSpreadB);
            pstmt.setDouble(40,SunflowerSpreadB_after);
            pstmt.setDouble(41,SunflowerSpreadC);
            pstmt.setDouble(42,SunflowerSpreadC_after);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
    
    //Insert data to database (Order transaction).
    public void orderTransaction(String tableName, int HamCheeseSandwichA_order, int HamCheeseSandwichA_accept, int HamCheeseSandwichB_order, int HamCheeseSandwichB_accept, int HamCheeseSandwichC_order, int HamCheeseSandwichC_accept,
    int HamSandwichA_order, int HamSandwichA_accept, int HamSandwichB_order, int HamSandwichB_accept, int HamSandwichC_order, int HamSandwichC_accept,
    int CheeseOnionA_order, int CheeseOnionA_accept, int CheeseOnionB_order, int CheeseOnionB_accept, int CheeseOnionC_order, int CheeseOnionC_accept,
    int CheesePickleA_order, int CheesePickleA_accept, int CheesePickleB_order, int CheesePickleB_accept, int CheesePickleC_order, int CHeesePickleC_accept,
    int TunaA_order, int TunaA_accept, int TunaB_order, int TunaB_accept, int TunaC_order, int TunaC_accept){

        String sql = String.format("INSERT INTO %s (HamCheeseSandwichA_order, HamCheeseSandwichA_accept, HamCheeseSandwichB_order, HamCheeseSandwichB_accept, HamCheeseSandwichC_order, HamCheeseSandwichC_accept,"+
        "HamSandwichA_order, HamSandwichA_accept, HamSandwichB_order, HamSandwichB_accept, HamSandwichC_order, HamSandwichC_accept," + 
        "CheeseOnionA_order, CheeseOnionA_accept, CheeseOnionB_order, CheeseOnionB_accept, CheeseOnionC_order, CheeseOnionC_accept," + 
        "CheesePickleA_order, CheesePickleA_accept, CheesePickleB_order, CheesePickleB_accept, CheesePickleC_order, CHeesePickleC_accept,"+
        "TunaA_order, TunaA_accept, TunaB_order, TunaB_accept, TunaC_order, TunaC_accept) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",tableName);
        try{
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1,HamCheeseSandwichA_order);
            pstmt.setInt(2,HamCheeseSandwichA_accept);
            pstmt.setInt(3, HamCheeseSandwichB_order);
            pstmt.setInt(4,HamCheeseSandwichB_accept);
            pstmt.setInt(5, HamCheeseSandwichC_order);
            pstmt.setInt(6,HamCheeseSandwichC_accept);
            pstmt.setInt(7,HamSandwichA_order);
            pstmt.setInt(8, HamSandwichA_accept);
            pstmt.setInt(9, HamSandwichB_order);
            pstmt.setInt(10,HamSandwichB_accept);
            pstmt.setInt(11, HamSandwichC_order);
            pstmt.setInt(12, HamSandwichC_accept);
            pstmt.setInt(13,CheeseOnionA_order);
            pstmt.setInt(14, CheeseOnionA_accept);
            pstmt.setInt(15,CheeseOnionB_order);
            pstmt.setInt(16, CheeseOnionB_accept);
            pstmt.setInt(17, CheeseOnionC_order);
            pstmt.setInt(18, CheeseOnionC_accept);
            pstmt.setInt(19, CheesePickleA_order);
            pstmt.setInt(20, CheesePickleA_accept);
            pstmt.setInt(21, CheesePickleB_order);
            pstmt.setInt(22, CheesePickleB_accept);
            pstmt.setInt(23, CheesePickleC_order);
            pstmt.setInt(24, CHeesePickleC_accept);
            pstmt.setInt(25, TunaA_order);
            pstmt.setInt(26, TunaA_accept);
            pstmt.setInt(27, TunaB_order);
            pstmt.setInt(28, TunaB_accept);
            pstmt.setInt(29, TunaC_order);
            pstmt.setInt(30, TunaC_accept);
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