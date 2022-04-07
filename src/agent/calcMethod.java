package agent;
import com.opencsv.CSVWriter;
import database.DatabaseConn;

import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class calcMethod {
    //ArrayList initialize
    ArrayList<advMatchingArray> extracOrderDataList = new ArrayList<>();
    ArrayList<String> queryResult = new ArrayList<>();

    //Related parameter and utility classes.

    DecimalFormat df = new DecimalFormat("#.##");
    Random rand = new Random();
    DatabaseConn app = new DatabaseConn();

    //Random value data.
    public int getRandIntRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max number must be more than min number");
        }
        return rand.nextInt((max - min) + 1) + min;
    }

    public double getRandDoubleRange(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("max number must be more than min number");
        }
        return rand.nextDouble() * (max - min) + min;
    }

    //Num of day conver to Mon-Sun.
    public String dayInWeek(int numOfday) {
        int result = numOfday % 7;
        String dayString = null;
        if(result == 0){
            dayString = "Sunday";
        }else if(result == 1){
            dayString = "Monday";
        }else if(result == 2){
            dayString = "Tuesday";
        }else if(result == 3){
            dayString = "Wednesday";
        }else if(result == 4){
            dayString = "Thursday";
        }else if(result == 5){
            dayString = "Friday";
        }else {
            dayString = "Saturday";
        }
        return dayString;
    }


    //Matching request order method.
    public int matchingOrder (ArrayList<calcMethod.supplierInfo> ingredientCurrentList, String orderName, String orderGrade, int orderQuantity){
        /**
        for(int i = 0; i < ingredientCurrentList.size();i++) {
        	System.out.println("   Ingrad :      " + ingredientCurrentList.get(i).toStringOutput());
        }
         */
        //Initialize data and array for calculation method.
        //ArrayList<advMatchingArray> extracOrderDataList = new ArrayList<>();

        //Getting ingredients data from database(one product needs many ingredients).
        ArrayList<String> queryResult = app.selectProduct(orderName);

        //Put the required ingredients to queue (ArrayList queue)
        //The orders has different require ingredients and need to calculate the data before put on queue.
        for(int i = 0; i < queryResult.size();i++){
            if(queryResult.get(i) != null){
                double tempQ = app.selectQuantity(orderName, queryResult.get(i));
                if(tempQ > 0){
                    //System.out.println("             " + orderName + "  " + queryResult.get(i) + "   "+ tempQ);
                    extracOrderDataList.add(new advMatchingArray(queryResult.get(i), orderGrade, (tempQ * orderQuantity), tempQ, 0, 0, 0, 0,0));
                    //System.out.println("   Upper array:      " + extracOrderDataList.get(i).toStringOutput());
                }
            }
        }
        Collections.sort(extracOrderDataList, new advMaximumToPrepareOrderSort());
        
        //Matching the required ingredient and current stock ingredients.
        int numOfIngradeInList = extracOrderDataList.size();
        while (numOfIngradeInList >0){
            numOfIngradeInList--;

            double tempIngradPerUnit = extracOrderDataList.get(numOfIngradeInList).numPerOneProduct;
            double tempOrderNeed = extracOrderDataList.get(numOfIngradeInList).numRequire;
            double tempStockSupplier = 0;
            for (int j = 0; j < ingredientCurrentList.size();j++){
                if(ingredientCurrentList.get(j).productName.equals(extracOrderDataList.get(numOfIngradeInList).ingredientName) && ingredientCurrentList.get(j).ingredientGrade.equals(extracOrderDataList.get(numOfIngradeInList).targetIngredientGrade)){
                    tempStockSupplier =  tempStockSupplier + ingredientCurrentList.get(j).numOfstock;
                }
            }
            //System.out.println("                     dddddd       " + tempStockSupplier);
            if(tempStockSupplier == 0){
                break;
            }else if(tempStockSupplier > tempOrderNeed){
                extracOrderDataList.get(numOfIngradeInList).maxTotalProducts = orderQuantity;
            }else{
                int currentResearvedProduct = (int)(tempStockSupplier/tempIngradPerUnit);
                extracOrderDataList.get(numOfIngradeInList).maxTotalProducts = extracOrderDataList.get(numOfIngradeInList).maxTotalProducts + currentResearvedProduct;
            }

            if(extracOrderDataList.get(numOfIngradeInList).maxTotalProducts > orderQuantity){
                extracOrderDataList.get(numOfIngradeInList).maxTotalProducts = orderQuantity;
            }
        }

        //the maximum order that can sell to customer (in maximum).
        Collections.sort(extracOrderDataList, new advMaximumToPrepareOrderSort());

        //Updating the available stock after finished order calculation.
        int outputResult = extracOrderDataList.get(0).maxTotalProducts;      //Initialize the maximum prepared order for customer.
        for(int i = 0;i < extracOrderDataList.size();i++){
            extracOrderDataList.get(i).maxTotalProducts = outputResult;
            double stockNeedPerIngrad = extracOrderDataList.get(i).maxTotalProducts * extracOrderDataList.get(i).numPerOneProduct;
            for(int j = 0;j < ingredientCurrentList.size();j++){
                if(extracOrderDataList.get(i).ingredientName.equals(ingredientCurrentList.get(j).productName) && extracOrderDataList.get(i).targetIngredientGrade.equals(ingredientCurrentList.get(j).ingredientGrade) && stockNeedPerIngrad > 0) {
                    double currentStockOnRow = ingredientCurrentList.get(j).numOfstock;
                    if(currentStockOnRow > stockNeedPerIngrad){
                        currentStockOnRow = currentStockOnRow - stockNeedPerIngrad;
                        stockNeedPerIngrad = 0;
                    }else{
                        stockNeedPerIngrad = stockNeedPerIngrad - currentStockOnRow;
                        currentStockOnRow = 0;
                    }
                    ingredientCurrentList.get(j).numOfstock = currentStockOnRow;
                } 
            }
        }
        

        /**
        if(outputResult != 0){
            for (int i = 0; i < extracOrderDataList.size(); i++) {
                for(int j = 0; j < ingredientCurrentList.size();j++) {
                    if(extracOrderDataList.get(i).ingredientName.equals(ingredientCurrentList.get(j).productName) &&
                            extracOrderDataList.get(i).ingredientGrade.equals(ingredientCurrentList.get(j).ingredientGrade)) {
                        ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - (extracOrderDataList.get(i).numPerOneProduct * outputResult);
                    }
                }
            }
        }
        int finalOutput = (int)outputResut;    
        */
        extracOrderDataList.clear();
        extracOrderDataList.trimToSize();
        return outputResult;
    }

    //Advance matching order which include the maximum value and roll over the ingredient grad to maximize order service.
    //Related method below.
    public int expiredDateCount(String ingredientName, LocalDate addedDate, int dayCount){
        int periodDay = app.getIngradLifePeriod(ingredientName);            //get num of expire date for each ingredient(getting from databased)
        LocalDate expiredDate = addedDate.plusDays(periodDay);              //The expired date based on the day of ingredient is added to refrigerator.
        //int numToExpiredDate  = Integer.parseInt(String.valueOf(ChronoUnit.DAYS.between(java.time.LocalDate.now(),expiredDate)));
        int numToExpiredDate  = Integer.parseInt(String.valueOf(ChronoUnit.DAYS.between(java.time.LocalDate.now().plusDays(dayCount),expiredDate)));
        return numToExpiredDate;
    }

    public int advMatchingOrder (ArrayList<calcMethod.supplierInfo> ingredientCurrentList, String orderName, String orderGrade, int orderQuantity){

        //Initialize data and array for calculation method.
        //ArrayList<advMatchingArray> extracOrderDataList = new ArrayList<>();

        //Matching order between ingredients need and order requirement.
        
        double numPerOneProduct = 0;          //Selecting ingredients quantity for the piece of product.
        double numRequire = 0;
        int maxReserved = 0;                          //Final result calculation.
        
        //Getting ingredients data from database.
        ArrayList<String> queryResult = app.selectProduct(orderName);
        for(int i=0; i < queryResult.size();i++){
            if(queryResult.get(i) != null){
            double gradeANumStock = 0, gradeBNumStock = 0, gradeCNumStock = 0;      //Initialize the all grade of ingredient numstock to calculate.
            double gradeAUsage = 0, gradeBUsage = 0, gradeCUsage = 0;
            int totalResearvedOrder = 0;
            
            numPerOneProduct = app.selectQuantity(orderName, queryResult.get(i));          //Selecting ingredients quantity for the piece of product.
            numRequire = numPerOneProduct * orderQuantity;
            double basedNumreq = numRequire;
            //System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx     " + numRequire);
            //extracOrderDataList.add(new advMatchingArray(queryResult.get(i), orderGrade, numRequire, numPerOneProduct, 0, 0, 0, 0));
            
            //looping to check avalable stock of ingredients.
            for(int j = 0; j < ingredientCurrentList.size(); j++) {
                if (ingredientCurrentList.get(j).productName.equals(queryResult.get(i))) {
                    String grade = ingredientCurrentList.get(j).ingredientGrade;
                    switch (grade) {
                        case "A":
                            gradeANumStock = gradeANumStock + ingredientCurrentList.get(j).numOfstock;
                            break;
                        case "B":
                            gradeBNumStock = gradeBNumStock + ingredientCurrentList.get(j).numOfstock;
                            break;
                        case "C":
                            gradeCNumStock = gradeCNumStock + ingredientCurrentList.get(j).numOfstock;
                            break;
                    }
                }
            }

            switch (orderGrade){
                case "A":
                    //System.out.println("Choose A: \n");
                    totalResearvedOrder = (int) (gradeANumStock/numPerOneProduct);
                    if(totalResearvedOrder >= orderQuantity){
                        gradeAUsage = numRequire;
                        totalResearvedOrder = orderQuantity;
                    }else {
                        gradeAUsage = totalResearvedOrder * numPerOneProduct;
                    }
                    extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,gradeAUsage,0,0,0,totalResearvedOrder));
                    break;

                case "B":
                    //System.out.println("Choose B: \n");
                    //Calculation mehtod on ingrad B grade.
                    totalResearvedOrder = (int) (gradeBNumStock/numPerOneProduct);
                    if(totalResearvedOrder >= orderQuantity){
                        gradeBUsage = orderQuantity * numPerOneProduct;
                        totalResearvedOrder = orderQuantity;
                        numRequire = numRequire - gradeBUsage;
                    }else {
                        gradeBUsage = totalResearvedOrder * numPerOneProduct;
                        numRequire = numRequire - gradeBUsage;
                    }

                    //calculation method on ingrad A grade.
                    if(numRequire > 0){
                        int tempAgradProduct = (int) (gradeANumStock/numPerOneProduct);
                        if(tempAgradProduct + totalResearvedOrder >= orderQuantity){
                            gradeAUsage = numRequire;
                            totalResearvedOrder = orderQuantity;
                            numRequire = numRequire - gradeAUsage;
                        }else{
                            gradeAUsage = tempAgradProduct * numPerOneProduct;
                            totalResearvedOrder = totalResearvedOrder + tempAgradProduct;
                            numRequire = numRequire - gradeAUsage;
                            }
                    }
                    
                    extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,gradeAUsage,gradeBUsage,0, 0,totalResearvedOrder));
                    break;

                case "C":
                    //System.out.println("Choose C: \n");
                    totalResearvedOrder = (int) (gradeCNumStock/numPerOneProduct);
                    if(totalResearvedOrder >= orderQuantity){
                        gradeCUsage = orderQuantity * numPerOneProduct;
                        totalResearvedOrder = orderQuantity;
                        numRequire = numRequire - gradeCUsage;
                    }else {
                        gradeCUsage = totalResearvedOrder * numPerOneProduct;
                        numRequire = numRequire - gradeCUsage;
                    }

                    //calculation method on ingrad B grade.
                    if(numRequire > 0){
                        int tempBgradeProduct = (int) (gradeBNumStock/numPerOneProduct);
                        if(tempBgradeProduct + totalResearvedOrder >= orderQuantity){
                            gradeBUsage = numRequire;
                            totalResearvedOrder = orderQuantity;
                            numRequire = numRequire - gradeBUsage;
                        }else{
                            gradeBUsage = tempBgradeProduct * numPerOneProduct;
                            totalResearvedOrder = totalResearvedOrder + tempBgradeProduct;
                            numRequire = numRequire - gradeBUsage;
                        }
                    }
                    
                    //calculation method on ingrad A grade.
                    if (numRequire > 0){
                        int tempAgradProduct = (int) (gradeANumStock/numPerOneProduct);
                        if(tempAgradProduct + totalResearvedOrder >= orderQuantity){
                            gradeAUsage = numRequire;
                            totalResearvedOrder = orderQuantity;
                            numRequire = numRequire - gradeAUsage;
                        }else{
                            gradeAUsage = tempAgradProduct * numPerOneProduct;
                            totalResearvedOrder = totalResearvedOrder + tempAgradProduct;
                            numRequire = numRequire - gradeAUsage;
                        }
                    }
                    
                    extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,gradeAUsage,gradeBUsage,gradeCUsage, 0,totalResearvedOrder));
                    break;
                }
            }
        }

        Collections.sort(extracOrderDataList, new advMaximumToPrepareOrderSort());      //Find out the maximum reserved product with sorted algorithm.
        /**
        for (advMatchingArray a : extracOrderDataList
        ) {
            System.out.println("    " + a.toStringOutput());
        }
         */

        //Relaxing order based on the maximum order reserved.
        for (int i = 0; i < extracOrderDataList.size();i++){
            maxReserved = extracOrderDataList.get(0).maxTotalProducts;
            if(maxReserved != extracOrderDataList.get(i).maxTotalProducts){
                double ingradDiff = (extracOrderDataList.get(i).maxTotalProducts * extracOrderDataList.get(i).numPerOneProduct) - (maxReserved * extracOrderDataList.get(i).numPerOneProduct);
                extracOrderDataList.get(i).maxTotalProducts = maxReserved;      //adding new max reserved order.
                extracOrderDataList.get(i).numRequire = maxReserved * extracOrderDataList.get(i).numPerOneProduct;
                
                //relaxing ingredient used.
                double[] tempArray = {extracOrderDataList.get(i).gradCRequire, extracOrderDataList.get(i).gradeBRequire, extracOrderDataList.get(i).gradARequire};
                int tempArraylen = tempArray.length;
                while (tempArraylen > 0){
                    tempArraylen--;
                    if((tempArray[tempArraylen] - ingradDiff) > 0) {
                        tempArray[tempArraylen] = tempArray[tempArraylen] - ingradDiff;
                        ingradDiff = 0;
                        break;
                    }else {
                        ingradDiff = ingradDiff - tempArray[tempArraylen];
                        tempArray[tempArraylen] = 0;
                        //tempArray[tempArraylen] = Math.abs(temp);
                    }
                }
                extracOrderDataList.get(i).gradCRequire = tempArray[0];
                extracOrderDataList.get(i).gradeBRequire = tempArray[1];
                extracOrderDataList.get(i).gradARequire = tempArray[2];
            }else {
                extracOrderDataList.get(i).numRequire = maxReserved * extracOrderDataList.get(i).numPerOneProduct;
            }
        }


         /**
        //Relaxing order based on the maximum order reserved.
        for (int i = 0; i < extracOrderDataList.size();i++){
            maxReserved = extracOrderDataList.get(0).maxTotalProducts;
            if(maxReserved != extracOrderDataList.get(i).maxTotalProducts){
                extracOrderDataList.get(i).maxTotalProducts = maxReserved;      //adding new max reserved order.
                double ingradDiff = (extracOrderDataList
                        .get(i).maxTotalProducts * extracOrderDataList.get(i).numPerOneProduct) - (maxReserved * extracOrderDataList.get(i).numPerOneProduct);
                extracOrderDataList.get(i).numRequire = maxReserved * extracOrderDataList.get(i).numPerOneProduct;
                double[] tempArray = {extracOrderDataList.get(i).gradCRequire, extracOrderDataList.get(i).gradeBRequire, extracOrderDataList.get(i).gradARequire};
                int tempArraylen = tempArray.length;
                while (tempArraylen > 0){
                    tempArraylen--;
                    double temp = ingradDiff - tempArray[tempArraylen];
                    if(temp > 0) {
                        tempArray[tempArraylen] = 0;
                        ingradDiff = temp;
                    }else {
                        tempArray[tempArraylen] = Math.abs(temp);
                        break;
                    }
                }
                extracOrderDataList.get(i).gradCRequire = tempArray[0];
                extracOrderDataList.get(i).gradeBRequire = tempArray[1];
                extracOrderDataList.get(i).gradARequire = tempArray[2];
            }else {
                extracOrderDataList.get(i).numRequire = maxReserved * extracOrderDataList.get(i).numPerOneProduct;
            }
        }
        */

        /**
        for (advMatchingArray a : extracOrderDataList
        ) {
            System.out.println("  \nAfter update data  " + a.toStringOutput());
        }
         */

        //Updating ingredients usage to current list of ingredient update.
        for (int i = 0; i < extracOrderDataList.size();i++){
            double aUpdate = extracOrderDataList.get(i).gradARequire;
            double bUpdate = extracOrderDataList.get(i).gradeBRequire;
            double cUpdate = extracOrderDataList.get(i).gradCRequire;
            for (int j = 0; j < ingredientCurrentList.size();j++){
                if(ingredientCurrentList.get(j).productName.equals(extracOrderDataList.get(i).ingredientName)){
                    String grade = ingredientCurrentList.get(j).ingredientGrade;
                    switch (grade){
                        case "A":
                            if(aUpdate > ingredientCurrentList.get(j).numOfstock){
                                aUpdate = aUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else{
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - aUpdate;
                                aUpdate = 0;
                            }
                            break;
                        case "B":
                            if(bUpdate > ingredientCurrentList.get(j).numOfstock){
                                bUpdate = bUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else{
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - bUpdate;
                                bUpdate = 0;
                            }
                            break;
                        case "C":
                            if(cUpdate > ingredientCurrentList.get(j).numOfstock){
                                cUpdate = cUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else{
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - cUpdate;
                                cUpdate = 0;
                            }
                            break;
                    }
                }
            }
        }
        /**
        //Updating ingredients usage to current list of ingredient update.
        for (int i = 0; i < extracOrderDataList.size();i++){
            double aUpdate = extracOrderDataList.get(i).gradARequire;
            double bUpdate = extracOrderDataList.get(i).gradeBRequire;
            double cUpdate = extracOrderDataList.get(i).gradCRequire;
            for (int j = 0; j < ingredientCurrentList.size();j++){
                if(ingredientCurrentList.get(j).productName.equals(extracOrderDataList.get(i).ingredientName)){
                    String grade = ingredientCurrentList.get(j).ingredientGrade;
                    switch (grade){
                        case "A":
                            ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - aUpdate;
                            break;
                        case "B":
                            ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - bUpdate;
                            break;
                        case "C":
                            ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - cUpdate;
                            break;
                    }
                }
            }
        }
         */
        
        /**
        for (supplierInfo a : ingredientCurrentList
        ) {
            System.out.println("  \nSupplier List Status  " + a.toStringOutput());
        }
         */

        extracOrderDataList.clear();
        extracOrderDataList.trimToSize();

        return maxReserved;
    }

    public int advWithExpireDate (ArrayList<calcMethod.supplierInfo> ingredientCurrentList, String orderName, String orderGrade, int orderQuantity, int dayCount){
        //ingredientCurrentList.trimToSize();
        //Initialize data and array for calculation method.
        //ArrayList<advMatchingArray> extracOrderDataList = new ArrayList<>();
        //Matching order between ingredients need and order requirement.
        
        double numPerOneProduct;
        double numRequire;
        int maxReserved = 0;                             //Final result calculation.
        
        /**
        for (supplierInfo a : ingredientCurrentList
        ) {
            System.out.println("  \nSupplier List Status (before)  " + a.toStringOutput());
        }
         */
        //Getting ingredients data from database.
        ArrayList<String> queryResult = app.selectProduct(orderName);
        //Put the required ingredients to queue (ArrayList queue)
        //The orders has different require ingredients and need to clean the data before put on queue.
        for(int i = 0; i < queryResult.size();i++){
            if(queryResult.get(i) != null){
                int totalResearvedOrder = 0;
                double gradeANumStock = 0, gradeBNumStock = 0, gradeCNumStock = 0,generalGradeNumStock = 0;      //Initialize the all grade of ingredient numstock to calculate.
                double gradeAUsage = 0, gradeBUsage = 0, gradeCUsage = 0, generalGradeUsage =0;
                numPerOneProduct = app.selectQuantity(orderName, queryResult.get(i));          //Selecting ingredients quantity for the piece of product.
                numRequire = numPerOneProduct * orderQuantity;
                double basedNumreq = numRequire;
                for(int j = 0; j < ingredientCurrentList.size(); j++) {
                    //looking for the expired data.
                    int expriedDate = expiredDateCount(ingredientCurrentList.get(j).productName,ingredientCurrentList.get(j).addedToStockDate, dayCount);
                    //System.out.println(String.format("name %S   grade %s     date to expire    :      %d",ingredientCurrentList.get(j).productName,ingredientCurrentList.get(j).ingredientGrade,expriedDate));

                    //adding the ingredient that perfect matching wiht order (same and higher grad that close to expired)
                    //The expired date is from refrigerators that are concerned about product shelf life. It means that the expired date on the sandwich company refrigerator is covered the final product shelf life when they sell it to customers.
                    if ((ingredientCurrentList.get(j).productName.equals(queryResult.get(i)) && ingredientCurrentList.get(j).ingredientGrade.equals(orderGrade) && expriedDate > 0) ||
                    (ingredientCurrentList.get(j).productName.equals(queryResult.get(i)) && ingredientCurrentList.get(j).ingredientGrade.equals(orderGrade) == false && expriedDate <= 2 && expriedDate > 0)) {
                        String grade = ingredientCurrentList.get(j).ingredientGrade;
                        switch (grade) {
                            case "A":
                                gradeANumStock = gradeANumStock + ingredientCurrentList.get(j).numOfstock;
                                break;
                            case "B":
                                gradeBNumStock = gradeBNumStock + ingredientCurrentList.get(j).numOfstock;
                                break;
                            case "C":
                                gradeCNumStock = gradeCNumStock + ingredientCurrentList.get(j).numOfstock;
                                break;
                            case "general":
                                generalGradeNumStock = generalGradeNumStock + ingredientCurrentList.get(j).numOfstock;
                        }
                        /**
                        if(gradeANumStock !=0 || gradeBNumStock !=0 || gradeCNumStock !=0){
                            System.out.println(String.format("     %f    %f     %f",gradeANumStock,gradeBNumStock,gradeCNumStock));
                        }else{
                            System.out.println("\nAll ingredients expired and do not sell any sandwich");
                        }
                         */
                    }
                }
                //System.out.printf("Grade A: %s     Grade B: %s       Grade C: %      Before matching      %s%n",gradeANumStock,gradeBNumStock,gradeCNumStock, orderName);
                
                switch (orderGrade){
                    case "A":
                        //System.out.println("Choose A: \n");
                        totalResearvedOrder = (int) (gradeANumStock/numPerOneProduct);
                        if(totalResearvedOrder >= orderQuantity){
                            gradeAUsage = numRequire;
                            totalResearvedOrder = orderQuantity;
                        }else {
                            gradeAUsage = totalResearvedOrder * numPerOneProduct;
                        }
                        extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,gradeAUsage,0,0,0, totalResearvedOrder));
                        break;
    
                    case "B":
                        //System.out.println("Choose B: \n");
                        //Calculation mehtod on ingrad B grade.
                        totalResearvedOrder = (int) (gradeBNumStock/numPerOneProduct);
                        if(totalResearvedOrder >= orderQuantity){
                            gradeBUsage = orderQuantity * numPerOneProduct;
                            totalResearvedOrder = orderQuantity;
                            numRequire = numRequire - gradeBUsage;
                        }else {
                            gradeBUsage = totalResearvedOrder * numPerOneProduct;
                            numRequire = numRequire - gradeBUsage;
                        }
    
                        //calculation method on ingrad A grade.
                        if(numRequire > 0){
                            int tempAgradProduct = (int) (gradeANumStock/numPerOneProduct);
                            if(tempAgradProduct + totalResearvedOrder >= orderQuantity){
                                gradeAUsage = numRequire;
                                totalResearvedOrder = orderQuantity;
                                numRequire = numRequire - gradeAUsage;
                            }else{
                                gradeAUsage = tempAgradProduct * numPerOneProduct;
                                totalResearvedOrder = totalResearvedOrder + tempAgradProduct;
                                numRequire = numRequire - gradeAUsage;
                                }
                        }
                        
                        extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,gradeAUsage,gradeBUsage,0,0, totalResearvedOrder));
                        break;
    
                    case "C":
                        //System.out.println("Choose C: \n");
                        totalResearvedOrder = (int) (gradeCNumStock/numPerOneProduct);
                        if(totalResearvedOrder >= orderQuantity){
                            gradeCUsage = orderQuantity * numPerOneProduct;
                            totalResearvedOrder = orderQuantity;
                            numRequire = numRequire - gradeCUsage;
                        }else {
                            gradeCUsage = totalResearvedOrder * numPerOneProduct;
                            numRequire = numRequire - gradeCUsage;
                        }
    
                        //calculation method on ingrad B grade.
                        if(numRequire > 0){
                            int tempBgradeProduct = (int) (gradeBNumStock/numPerOneProduct);
                            if(tempBgradeProduct + totalResearvedOrder >= orderQuantity){
                                gradeBUsage = numRequire;
                                totalResearvedOrder = orderQuantity;
                                numRequire = numRequire - gradeBUsage;
                            }else{
                                gradeBUsage = tempBgradeProduct * numPerOneProduct;
                                totalResearvedOrder = totalResearvedOrder + tempBgradeProduct;
                                numRequire = numRequire - gradeBUsage;
                            }
                        }
                    
                        //calculation method on ingrad A grade.
                        if (numRequire > 0){
                            int tempAgradProduct = (int) (gradeANumStock/numPerOneProduct);
                            if(tempAgradProduct + totalResearvedOrder >= orderQuantity){
                                gradeAUsage = numRequire;
                                totalResearvedOrder = orderQuantity;
                                numRequire = numRequire - gradeAUsage;
                            }else{
                                gradeAUsage = tempAgradProduct * numPerOneProduct;
                                totalResearvedOrder = totalResearvedOrder + tempAgradProduct;
                                numRequire = numRequire - gradeAUsage;
                            }
                        }
                        
                        extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,gradeAUsage,gradeBUsage,gradeCUsage, 0, totalResearvedOrder));
                        break;

                    case "general":
                        //System.out.println("Choose general: \n");
                        totalResearvedOrder = (int) (generalGradeNumStock/numPerOneProduct);
                        if(totalResearvedOrder >= orderQuantity){
                            generalGradeUsage = numRequire;
                            totalResearvedOrder = orderQuantity;
                        }else {
                            generalGradeUsage = totalResearvedOrder * numPerOneProduct;
                        }
                        extracOrderDataList.add(new advMatchingArray(queryResult.get(i),orderGrade, basedNumreq,numPerOneProduct,0,0,0, generalGradeUsage, totalResearvedOrder));
                        break;
                    
                }
            }
        }
        Collections.sort(extracOrderDataList, new advMaximumToPrepareOrderSort());      //Find out the maximum reserved product with sorted algorithm.
        /**
        for (advMatchingArray a : extracOrderDataList
        ) {
            System.out.println("    " + a.toStringOutput());
        }
         */
        
        //Relaxing order based on the maximum order reserved.
        for (int i = 0; i < extracOrderDataList.size();i++){
            maxReserved = extracOrderDataList.get(0).maxTotalProducts;
            if(maxReserved != extracOrderDataList.get(i).maxTotalProducts){
                double ingradDiff = (extracOrderDataList.get(i).maxTotalProducts * extracOrderDataList.get(i).numPerOneProduct) - (maxReserved * extracOrderDataList.get(i).numPerOneProduct);
                extracOrderDataList.get(i).maxTotalProducts = maxReserved;      //adding new max reserved order.
                extracOrderDataList.get(i).numRequire = maxReserved * extracOrderDataList.get(i).numPerOneProduct;
                
                //relaxing ingredient used.
                double[] tempArray = {extracOrderDataList.get(i).gradCRequire, extracOrderDataList.get(i).gradeBRequire, extracOrderDataList.get(i).gradARequire, extracOrderDataList.get(i).generalGradeRequire};
                int tempArraylen = tempArray.length;
                while (tempArraylen > 0){
                    tempArraylen--;
                    if((tempArray[tempArraylen] - ingradDiff) > 0) {
                        tempArray[tempArraylen] = tempArray[tempArraylen] - ingradDiff;
                        ingradDiff = 0;
                        break;
                    }else {
                        ingradDiff = ingradDiff - tempArray[tempArraylen];
                        tempArray[tempArraylen] = 0;
                        //tempArray[tempArraylen] = Math.abs(temp);
                    }
                }
                extracOrderDataList.get(i).gradCRequire = tempArray[0];
                extracOrderDataList.get(i).gradeBRequire = tempArray[1];
                extracOrderDataList.get(i).gradARequire = tempArray[2];
                extracOrderDataList.get(i).generalGradeRequire = tempArray[3];
            }else {
                extracOrderDataList.get(i).numRequire = maxReserved * extracOrderDataList.get(i).numPerOneProduct;

            }
        }
        //Usaged data update to supplierList.
        for (int i = 0; i < extracOrderDataList.size();i++){
            double aUpdate = extracOrderDataList.get(i).gradARequire;
            double bUpdate = extracOrderDataList.get(i).gradeBRequire;
            double cUpdate = extracOrderDataList.get(i).gradCRequire;
            double generalUpdate = extracOrderDataList.get(i).generalGradeRequire;

            for (int j = 0; j < ingredientCurrentList.size();j++){
                if(ingredientCurrentList.get(j).productName.equals(extracOrderDataList.get(i).ingredientName)){
                    String grade = ingredientCurrentList.get(j).ingredientGrade;
                    switch (grade){
                        case "A":
                            if(aUpdate > ingredientCurrentList.get(j).numOfstock){
                                aUpdate = aUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else{
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - aUpdate;
                                aUpdate = 0;
                            }
                            break;
                        case "B":
                            if(bUpdate > ingredientCurrentList.get(j).numOfstock){
                                bUpdate = bUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else{
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - bUpdate;
                                bUpdate = 0;
                            }
                            break;
                        case "C":
                            if(cUpdate > ingredientCurrentList.get(j).numOfstock){
                                cUpdate = cUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else{
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - cUpdate;
                                cUpdate = 0;
                            }
                            break;
                        case "general":
                            if(generalUpdate > ingredientCurrentList.get(j).numOfstock){
                                generalUpdate = generalUpdate - ingredientCurrentList.get(j).numOfstock;
                                ingredientCurrentList.get(j).numOfstock = 0;
                            }else {
                                ingredientCurrentList.get(j).numOfstock = ingredientCurrentList.get(j).numOfstock - generalUpdate;
                            }
                            break;
                    }
                }
            }
        }
        /**
        for (advMatchingArray a : extracOrderDataList
        ) {
            System.out.println("  \nAfter update data  " + a.toStringOutput());
        }
         */
        //Updating ingredients usage to current list of ingredient update.

        /**
        for (supplierInfo a : ingredientCurrentList
        ) {
            System.out.println("  \nSupplier List Status  " + a.toStringOutput());
        }
         */

        return maxReserved;
    }

    public int newMarketMatching (ArrayList<calcMethod.supplierInfo> ingredientCurrentList, String orderName, String orderGrade, int orderQuantity, int dayCount){
        //ingredientCurrentList.trimToSize();
        //Initialize data and array for calculation method.
        //ArrayList<advMatchingArray> extracOrderDataList = new ArrayList<>();
        //Matching order between ingredients need and order requirement.
        HashMap<String,Double> numPeringradList = new HashMap<>();
        int maxReserved = orderQuantity;                             //Final production request.
        /**
         for (supplierInfo a : ingredientCurrentList
         ) {
         System.out.println("  \nSupplier List Status (before)  " + a.toStringOutput());
         }
         */
        //Getting ingredients data from database.
        ArrayList<String> queryResult = app.selectProduct(orderName);
        for(int i = 0; i < queryResult.size();i++){
            if(queryResult.get(i) != null){
                double numPerOneProduct = app.selectQuantity(orderName, queryResult.get(i));          //Selecting ingredients quantity for the piece of product.
                numPeringradList.put(queryResult.get(i),numPerOneProduct);
            }
        }

        ArrayList<Integer> maxProduceArray = new ArrayList<Integer>();
        for(Map.Entry<String,Double> pair : numPeringradList.entrySet()) {
            String ingradName = pair.getKey();
            double numPerOneProduct = pair.getValue();
            double numReq = numPerOneProduct * orderQuantity;
            double totalCurrentIngrad = 0;
            for (int j = 0; j < ingredientCurrentList.size(); j++) {
                if (ingredientCurrentList.get(j).productName.equals(ingradName)) {
                    totalCurrentIngrad = totalCurrentIngrad + ingredientCurrentList.get(j).numOfstock;
                }
            }
            System.out.println(String.format("\n total Current ingrad: %s    stock: %.2f",ingradName,totalCurrentIngrad));

            //Maximum product calculation
            int maxProduce = (int) (totalCurrentIngrad/numPerOneProduct);
            maxProduceArray.add(maxProduce);
            System.out.print(String.format("\n max produce for %s    is  %d",ingradName,maxProduce));
            if(maxProduce < maxReserved){
                maxReserved = maxProduce;
            }
        }

        System.out.println("\n Max reserve is " + maxReserved);

        /*
        //Put the required ingredients to queue (ArrayList queue)
        //The orders has different require ingredients and need to clean the data before put on queue.
        for(int i = 0; i < queryResult.size();i++){
            if(queryResult.get(i) != null){
                double generalGradeNumStock = 0;    //Initialize the all grade of ingredient numstock to calculate.
                //adding ingradList with numPerProduct to Hash table.
                double numPerOneProduct = app.selectQuantity(orderName, queryResult.get(i));          //Selecting ingredients quantity for the piece of product.
                numPeringradList.put(queryResult.get(i),numPerOneProduct);
                for(int j = 0; j < ingredientCurrentList.size(); j++) {
                    //looking for the expired data.
                    int expriedDate = expiredDateCount(ingredientCurrentList.get(j).productName,ingredientCurrentList.get(j).addedToStockDate, dayCount);
                    //adding the ingredient that perfect matching wiht order (same and higher grad that close to expired)
                    //The expired date is from refrigerators that are concerned about product shelf life. It means that the expired date on the sandwich company refrigerator is covered the final product shelf life when they sell it to customers.
                    if (ingredientCurrentList.get(j).productName.equals(queryResult.get(i)) && expriedDate > 0) {
                        generalGradeNumStock = generalGradeNumStock + ingredientCurrentList.get(j).numOfstock;
                    }
                    //System.out.println("current Stock: " + queryResult.get(i) + "     " + generalGradeNumStock);
                }
                int tempReserveOrder = (int) (generalGradeNumStock/numPerOneProduct);
                //System.out.println(" Max Reserve: " + tempReserveOrder);
                if (tempReserveOrder < maxReserved){
                    maxReserved = tempReserveOrder;         //Maximize max order production based on current stock ingredients.
                }
            }
        }
        */
        //Produced order and update the current ingredient stock after production process.(modified relaxing list)
        for(Map.Entry<String,Double> pair : numPeringradList.entrySet()){
            String ingradName = pair.getKey();
            double numPerOneProduct = pair.getValue();
            double numReq = numPerOneProduct * maxReserved;
            while (numReq > 0){
                for(int i = 0; i < ingredientCurrentList.size();i++){
                    if(ingredientCurrentList.get(i).productName.equals(ingradName)){
                        if(ingredientCurrentList.get(i).numOfstock - numReq > 0){
                            ingredientCurrentList.get(i).numOfstock = ingredientCurrentList.get(i).numOfstock - numReq;
                            numReq = 0;
                        }else {
                            numReq = numReq - ingredientCurrentList.get(i).numOfstock;
                            ingredientCurrentList.get(i).numOfstock = 0;
                        }
                    }
                }
            }
        }

        return maxReserved;
    }


    public customerInfo randCustomerInput(String agentName) {
        //Random related variable
        String[] randOrder = app.selectProductRandom();
        String orderName = randOrder[0];
        String productGrade = randOrder[1];
        double pricePerUnit = Double.parseDouble(randOrder[2]);
        /***
        ArrayList<String> productNameList = new ArrayList<>(Arrays.asList("HamSandwich", "HamCheeseSandwich"));
        ArrayList<String> productGradeList = new ArrayList<>(Arrays.asList("A", "B", "C"));
        int orderNameNum = rand.nextInt(productNameList.size());
        String orderName = productNameList.get(orderNameNum);
        int productGradeNum = rand.nextInt(productGradeList.size());
        String productGrade = productGradeList.get(productGradeNum);
         ***/
        int numOfRequire = getRandIntRange(100,5000);
        int numReply = 0;
        customerInfo output = new customerInfo(agentName,orderName,productGrade,numOfRequire, pricePerUnit, numReply,0,0);

        return output;
        }

    public supplierInfo randSupplierInput(String agentName, String ingredientName, String grade, int ingradRandPolicy) {
        //Random related variable
        //ArrayList<String> ingredientsList = new ArrayList<>(Arrays.asList("Ham, Cheese, Bread, Lettuce"));
        //ArrayList<String> qualityList = new ArrayList<String>(Arrays.asList("A", "B", "C"));
        //int ingredientsNum = rand.nextInt(ingredientsList.size());
        //String ingredientName = ingredientsList.get(ingredientsNum);
        //int qualityNum = rand.nextInt(qualityList.size());
        //String quality = qualityList.get(qualityNum);

        //ingradRandPolicy = getRandIntRange(1,3);
        //int numStockRand = 1;
        int numOfstock = 0;
        
        //int numOfstock = getRandIntRange(numStockRand * 1000000,numStockRand * 6000000);
        
        //adding the whitebread double and triple time stock.
        switch(ingradRandPolicy){
            case 0:                         //no need to refill the ingredient stocks
                numOfstock = 0;
                break;
            case 1:
                numOfstock = 1000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;
            case 2:
                numOfstock = 2000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;

            case 3:
                numOfstock = 3000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;
            
            case 4:
                numOfstock = 4000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;

            case 5:
                numOfstock = 5000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;

            case 6:
                numOfstock = 6000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;

            case 7: 
                numOfstock = 7000000;
                if(ingredientName == "WhiteBread"){
                    numOfstock = numOfstock * 2;
                }
                break;
        }
        
            
        
        supplierInfo output = new supplierInfo(agentName, ingredientName, grade, numOfstock,java.time.LocalDate.now());

        return output;
    }
    
    //Utility function (simple calculation based on the price and value added from all customer request).
    public double utilityValueCalculation(int utilityMethod, int numOfOrder, double pricePerUnit, double productCost) {
        double output = 0.0;
        switch (utilityMethod){
            case 0:
                //System.out.println("The utility method is default");
                output = numOfOrder * pricePerUnit;
                break;
            case 1:
                //System.out.println("The utility method is marginal profit value");
                double margin = pricePerUnit - productCost;
                output = numOfOrder * margin;
                break;
        }
    	
    	return output;
    }

    public void orderTransactionReq(String orderName, String grade, int numOfOrder, ArrayList<calcMethod.orderTransaction> resultArray){
        //System.out.print("requst to write the order requirement transaction");
        switch(orderName){
            case "HamCheeseSandwich":
                if(grade.equals("A")){
                    resultArray.get(0).HamCheeseSandwichA_order = resultArray.get(0).HamCheeseSandwichA_order + numOfOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).HamCheeseSandwichB_order = resultArray.get(0).HamCheeseSandwichB_order + numOfOrder;    
                }else{
                    resultArray.get(0).HamCheeseSandwichC_order = resultArray.get(0).HamCheeseSandwichC_order + numOfOrder;
                }
                break;

            case "HamSandwich":
                if(grade.equals("A")){
                    resultArray.get(0).HamSandwichA_order = resultArray.get(0).HamSandwichA_order + numOfOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).HamSandwichB_order = resultArray.get(0).HamSandwichB_order + numOfOrder;    
                }else{
                    resultArray.get(0).HamSandwichC_order = resultArray.get(0).HamSandwichC_order + numOfOrder;
                }
                break;

            case "CheeseOnion":
                if(grade.equals("A")){
                    resultArray.get(0).CheeseOnionA_order = resultArray.get(0).CheeseOnionA_order + numOfOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).CheeseOnionB_order = resultArray.get(0).CheeseOnionB_order + numOfOrder;    
                }else{
                    resultArray.get(0).CheeseOnionC_order = resultArray.get(0).CheeseOnionC_order + numOfOrder;
                }
                break;

            case "CheesePickle":
                if(grade.equals("A")){
                    resultArray.get(0).CheesePickleA_order = resultArray.get(0).CheesePickleA_order + numOfOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).CheesePickleB_order = resultArray.get(0).CheesePickleB_order + numOfOrder;    
                }else{
                    resultArray.get(0).CheesePickleC_order = resultArray.get(0).CheesePickleC_order + numOfOrder;
                }
                break;

            case "Tuna":
                if(grade.equals("A")){
                    resultArray.get(0).TunaA_order = resultArray.get(0).TunaA_order + numOfOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).TunaB_order = resultArray.get(0).TunaB_order + numOfOrder;    
                }else{
                    resultArray.get(0).TunaC_order = resultArray.get(0).TunaC_order + numOfOrder;
                }
                break;
        }
    }

    public void orderTransactionAccept(String orderName, String grade, int acceptOrder, ArrayList<calcMethod.orderTransaction> resultArray){
        //System.out.print("requst to write the order requirement transaction");
        switch(orderName){
            case "HamCheeseSandwich":
                if(grade.equals("A")){
                    resultArray.get(0).HamCheeseSandwichA_accept = resultArray.get(0).HamCheeseSandwichA_accept + acceptOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).HamCheeseSandwichB_accept = resultArray.get(0).HamCheeseSandwichB_accept + acceptOrder;    
                }else{
                    resultArray.get(0).HamCheeseSandwichC_accept = resultArray.get(0).HamCheeseSandwichC_accept + acceptOrder;
                }
                break;

            case "HamSandwich":
                if(grade.equals("A")){
                    resultArray.get(0).HamSandwichA_accept = resultArray.get(0).HamSandwichA_accept + acceptOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).HamSandwichB_accept = resultArray.get(0).HamSandwichB_accept + acceptOrder;    
                }else{
                    resultArray.get(0).HamSandwichC_accept = resultArray.get(0).HamSandwichC_accept + acceptOrder;
                }
                break;

            case "CheeseOnion":
                if(grade.equals("A")){
                    resultArray.get(0).CheeseOnionA_accept = resultArray.get(0).CheeseOnionA_accept + acceptOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).CheeseOnionB_accept = resultArray.get(0).CheeseOnionB_accept + acceptOrder;    
                }else{
                    resultArray.get(0).CheeseOnionC_accept = resultArray.get(0).CheeseOnionC_accept + acceptOrder;
                }
                break;

            case "CheesePickle":
                if(grade.equals("A")){
                    resultArray.get(0).CheesePickleA_accept = resultArray.get(0).CheeseOnionA_accept + acceptOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).CheesePickleB_accept = resultArray.get(0).CheesePickleB_accept + acceptOrder;    
                }else{
                    resultArray.get(0).CheesePickleC_accept = resultArray.get(0).CheesePickleC_accept + acceptOrder;
                }
                break;

            case "Tuna":
                if(grade.equals("A")){
                    resultArray.get(0).TunaA_accept = resultArray.get(0).TunaA_accept + acceptOrder;
                }else if(grade.equals("B")){
                    resultArray.get(0).TunaB_accept = resultArray.get(0).TunaB_accept + acceptOrder;    
                }else{
                    resultArray.get(0).TunaC_accept = resultArray.get(0).TunaC_accept + acceptOrder;
                }
                break;
        }
    }

    public void ingradientTransactionBefore(String ingredientName, String grade, double quantity, ArrayList<calcMethod.ingredientTransaction> resultArray){
        switch(ingredientName){
            case "WhiteBread":
                if(grade.equals("A")){
                    resultArray.get(0).WhiteBreadA = resultArray.get(0).WhiteBreadA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).WhiteBreadB = resultArray.get(0).WhiteBreadB + quantity;    
                }else{
                    resultArray.get(0).WhiteBreadC = resultArray.get(0).WhiteBreadC + quantity;
                }
                break;

            case "Ham":
                if(grade.equals("A")){
                    resultArray.get(0).HamA = resultArray.get(0).HamA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).HamB = resultArray.get(0).HamB + quantity;    
                }else{
                    resultArray.get(0).HamC = resultArray.get(0).HamC + quantity;
                }
                break;

            case "MatureCheddarMilk":
                if(grade.equals("A")){
                    resultArray.get(0).MatureCheddarMilkA = resultArray.get(0).MatureCheddarMilkA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).MatureCheddarMilkB = resultArray.get(0).MatureCheddarMilkB + quantity;    
                }else{
                    resultArray.get(0).MatureCheddarMilkC = resultArray.get(0).MatureCheddarMilkC + quantity;
                }
                break;

            case "Onion":
                if(grade.equals("A")){
                    resultArray.get(0).OnionA = resultArray.get(0).OnionA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).OnionB = resultArray.get(0).OnionB + quantity;    
                }else{
                    resultArray.get(0).OnionC = resultArray.get(0).OnionC + quantity;
                }
                break;

            case "Pickle":
                if(grade.equals("A")){
                    resultArray.get(0).PickleA = resultArray.get(0).PickleA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).PickleB = resultArray.get(0).PickleB + quantity;    
                }else{
                    resultArray.get(0).PickleC = resultArray.get(0).PickleC + quantity;
                }
                break;

            case "Tuna":
                if(grade.equals("A")){
                    resultArray.get(0).TunaA = resultArray.get(0).TunaA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).TunaB = resultArray.get(0).TunaB + quantity;    
                }else{
                    resultArray.get(0).TunaC = resultArray.get(0).TunaC + quantity;
                }
                break;

            case "SunflowerSpread":
                if(grade.equals("A")){
                    resultArray.get(0).SunflowerSpreadA = resultArray.get(0).SunflowerSpreadA + quantity;
                }else if(grade.equals("B")){
                    resultArray.get(0).SunflowerSpreadB = resultArray.get(0).SunflowerSpreadB + quantity;    
                }else{
                    resultArray.get(0).SunflowerSpreadC = resultArray.get(0).SunflowerSpreadC + quantity;
                }
                break;
        }
    }

    public void ingradientTransactionAfter(String ingredientName, String ingredientGrade, double quantity, ArrayList<calcMethod.ingredientTransaction> resultArray){
        switch(ingredientName){
            case "WhiteBread":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).WhiteBreadA_after = resultArray.get(0).WhiteBreadA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).WhiteBreadB_after = resultArray.get(0).WhiteBreadB_after + quantity;    
                }else{
                    resultArray.get(0).WhiteBreadC_after = resultArray.get(0).WhiteBreadC_after + quantity;
                }
                break;
                
            case "Ham":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).HamA_after = resultArray.get(0).HamA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).HamB_after = resultArray.get(0).HamB_after + quantity;    
                }else{
                    resultArray.get(0).HamC_after = resultArray.get(0).HamC_after + quantity;
                }
                break;

            case "MatureCheddarMilk":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).MatureCheddarMilkA_after = resultArray.get(0).MatureCheddarMilkA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).MatureCheddarMilkB_after = resultArray.get(0).MatureCheddarMilkB_after + quantity;    
                }else{
                    resultArray.get(0).MatureCheddarMilkC_after = resultArray.get(0).MatureCheddarMilkC_after + quantity;
                }
                break;
            
            case "Onion":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).OnionA_after = resultArray.get(0).OnionA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).OnionB_after = resultArray.get(0).OnionB_after + quantity;    
                }else{
                    resultArray.get(0).OnionC_after = resultArray.get(0).OnionC_after + quantity;
                }
                break;

            case "Pickle":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).PickleA_after = resultArray.get(0).PickleA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).PickleB_after = resultArray.get(0).PickleB_after + quantity;    
                }else{
                    resultArray.get(0).PickleC_after = resultArray.get(0).PickleC_after + quantity;
                }
                break;

            case "Tuna":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).TunaA_after = resultArray.get(0).TunaA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).TunaB_after = resultArray.get(0).TunaB_after + quantity;    
                }else{
                    resultArray.get(0).TunaC_after = resultArray.get(0).TunaC_after + quantity;
                }
                break;

            case "SunflowerSpread":
                if(ingredientGrade.equals("A")){
                    resultArray.get(0).SunflowerSpreadA_after = resultArray.get(0).SunflowerSpreadA_after + quantity;
                }else if(ingredientGrade.equals("B")){
                    resultArray.get(0).SunflowerSpreadB_after = resultArray.get(0).SunflowerSpreadB_after + quantity;    
                }else{
                    resultArray.get(0).SunflowerSpreadC_after = resultArray.get(0).SunflowerSpreadC_after + quantity;
                }
                break;
        }
    }

    //Writing output method.
    public void createCSV(String fileDirectory,String[] recordName) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(fileDirectory));
        String[] recordRow = recordName;
        writer.writeNext(recordRow);
        writer.close();
    }

    public void updateCSVFile(String fileDirectory, String[] dataInRow) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(fileDirectory,true));
        String[] recordRow = dataInRow;
        writer.writeNext(recordRow);
        writer.close();
    }

    class maximumToPrepareOrderSort implements Comparator<matchingTalbleArray> {
        //Used for sorting in ascending order of the volume.
        public int compare(matchingTalbleArray a, matchingTalbleArray b) {
            return Double.compare(a.maxTotalProducts, b.maxTotalProducts);
        }
    }

    class advMaximumToPrepareOrderSort implements Comparator<advMatchingArray> {
        //Used for sorting in ascending order of the volume.
        public int compare(advMatchingArray a, advMatchingArray b) {
            return Double.compare(a.maxTotalProducts, b.maxTotalProducts);
        }
    }

    //Sorted customer array to estimation
    static class sortedByNumOfOrder implements Comparator<customerInfo> {
        //Used for sorting in ascending order of the volume.
        public int compare(customerInfo a, customerInfo b) {
            return Double.compare(a.numOfOrder, b.numOfOrder);
        }
    }

    static class sortedByUtilityValue implements Comparator<customerInfo> {
        //Used for sorting in ascending order of the volume.
        public int compare(customerInfo a, customerInfo b) {
            return Double.compare(a.utilityValue, b.utilityValue);
        }
    }

    //Stock checklist
    public class productChecklist{
        public String productName;
        public String productGrade;
        public int numOfProductReq;
        public int numOfCurrentStock;

        public productChecklist(String productName, String productGrade, int numOfProductReq, int numOfCurrentStock){
            this.productName = productName;
            this.productGrade = productGrade;
            this.numOfProductReq = numOfProductReq;
            this.numOfCurrentStock = numOfCurrentStock;
        }
        public String toStringOutput() {
            return "Ingredient name: " + this.productName + "   Quality: " + this.productGrade + "   Stock required for this order: " + this.numOfProductReq +
                    "  Current from stock: " + this.numOfCurrentStock;
        }
    }

    //Matching table to compare and check stock available
    public class matchingTalbleArray{
        public String ingredientName;
        public String ingredientGrade;
        public Double numRequire;  //ingredient requirement for the order
        //public Double numCurrentInStock;  //current stock which count from the list of ingredients.
        public Double numPerOneProduct;    //How many for one piece of sandwich.
        public int maxTotalProducts;    //Maximum orders to prepare the pice of product.

        public matchingTalbleArray(String ingredientName, String ingredientGrade, double numRequire, double numPerOneProduct, int maxTotalProducts) {
            this.ingredientName = ingredientName;
            this.ingredientGrade = ingredientGrade;
            this.numRequire = numRequire;
            //this.numCurrentInStock = numCurrentInStock;
            this.numPerOneProduct = numPerOneProduct;
            this.maxTotalProducts = maxTotalProducts;
        }
        
        public String toStringOutput() {
            return "Ingredient name: " + this.ingredientName + "   Quality: " + this.ingredientGrade + "   Stock required for this order: " + this.numRequire + "  num per 1 product: " + this.numPerOneProduct + "  Maximum pieces for total order: " + df.format(this.maxTotalProducts);
            /***
            return "Ingredient name: " + this.ingredientName + "   Quality: " + this.ingredientGrade + "   Stock required for this order: " + this.numRequire +
            		"  Current from stock: " + this.numCurrentInStock + "  num per 1 product: " + this.numPerOneProduct + "  Maximum pieces for total order: " + df.format(this.maxTotalProducts);
             ***/
        }
    }

    //Matching table to compare and check stock available
    public class advMatchingArray{
        public String ingredientName;
        public String targetIngredientGrade;
        public double numRequire;  //ingredient requirement for the order
        public double numPerOneProduct;    //How many for one piece of sandwich.
        public double gradARequire;
        public double gradeBRequire;
        public double gradCRequire;
        public double generalGradeRequire;
        public int maxTotalProducts;    //Maximum orders to prepare the pice of product.

        public advMatchingArray(String ingredientName, String targetIngredientGrade, double numRequire, double numPerOneProduct, double gradARequire, double gradeBRequire,
                                double gradCRequire, double generalGradeRequire,int maxTotalProducts) {
            this.ingredientName = ingredientName;
            this.targetIngredientGrade = targetIngredientGrade;
            this.numRequire = numRequire;
            this.numPerOneProduct = numPerOneProduct;
            this.gradARequire = gradARequire;
            this.gradeBRequire = gradeBRequire;
            this.gradCRequire = gradCRequire;
            this.generalGradeRequire = generalGradeRequire;
            this.maxTotalProducts = maxTotalProducts;
        }

        public String toStringOutput() {
            return String.format("Ingredient name: %s Quality needed: %s Stock require for this order: %.02f num of each unit: %.02f A grade needed: %.02f B grade needed: %.02f C grade needed: %.02f Total product can made: %d",
                    this.ingredientName, this.targetIngredientGrade, this.numRequire, this.numPerOneProduct, this.gradARequire, this.gradeBRequire, this.gradCRequire, this.generalGradeRequire, this.maxTotalProducts);
            /***
             return "Ingredient name: " + this.ingredientName + "   Quality: " + this.ingredientGrade + "   Stock required for this order: " + this.numRequire +
             "  Current from stock: " + this.numCurrentInStock + "  num per 1 product: " + this.numPerOneProduct + "  Maximum pieces for total order: " + df.format(this.maxTotalProducts);
             ***/
        }
    }

    public class advMatchWithExipredataTable{
        public String ingredientName;
        public String targetIngredientGrade;
        public double numRequire;  //ingredient requirement for the order
        public double numPerOneProduct;    //How many for one piece of sandwich.
        public double gradARequire;
        public double gradeBRequire;
        public double gradCRequire;
        public int maxTotalProducts;    //Maximum orders to prepare the pice of product.

        public advMatchWithExipredataTable(String ingredientName, String targetIngredientGrade, double numRequire, double numPerOneProduct, double gradARequire, double gradeBRequire,
                                double gradCRequire, int maxTotalProducts) {
            this.ingredientName = ingredientName;
            this.targetIngredientGrade = targetIngredientGrade;
            this.numRequire = numRequire;
            this.numPerOneProduct = numPerOneProduct;
            this.gradARequire = gradARequire;
            this.gradeBRequire = gradeBRequire;
            this.gradCRequire = gradCRequire;
            this.maxTotalProducts = maxTotalProducts;
        }

        public String toStringOutput() {
            return String.format("Ingredient name: %s Quality needed: %s Stock require for this order: %.02f num of each unit: %.02f A grade needed: %.02f B grade needed: %.02f C grade needed: %.02f Total product can made: %d",
                    this.ingredientName, this.targetIngredientGrade, this.numRequire, this.numPerOneProduct, this.gradARequire, this.gradeBRequire, this.gradCRequire, this.maxTotalProducts);
            /***
             return "Ingredient name: " + this.ingredientName + "   Quality: " + this.ingredientGrade + "   Stock required for this order: " + this.numRequire +
             "  Current from stock: " + this.numCurrentInStock + "  num per 1 product: " + this.numPerOneProduct + "  Maximum pieces for total order: " + df.format(this.maxTotalProducts);
             ***/
        }
    }

    public class supplierInfo{
        public String agentName;
        public String productName;
        public String ingredientGrade;
        public double numOfstock;
        public LocalDate addedToStockDate;

        public supplierInfo(String agentName, String productName, String ingredientGrade, double numOfstock, LocalDate addedToStockDate) {
            this.agentName = agentName;
            this.productName = productName;
            this.ingredientGrade = ingredientGrade;
            this.numOfstock = numOfstock;
            this.addedToStockDate = addedToStockDate;
        }

        public String toStringOutput() {
            return "Agent name: "+ this.agentName + " Ingredient name: " + this.productName + "   Quality: " + this.ingredientGrade + "   Stock: " + df.format(numOfstock) + "  Added to stock: " + this.addedToStockDate.toString();
        }
        public String toUpdateService(){
            return this.productName + "-" + this.ingredientGrade + "-" + this.numOfstock + "-" + this.addedToStockDate;
        }
    }
    
    public class customerInfo{
        public String agentName;
        public String orderName;
        public String ingredientGrade;
        public int numOfOrder;
        public double pricePerUnit;
        public int numReply;
        public int orderStatus;
        public double utilityValue;

        public customerInfo(String agentName, String orderName, String ingredientGrade, int numOfOrder, double pricePerUnit,int numReply, int orderStatus, double utilityValue) {
            this.agentName = agentName;
            this.orderName = orderName;
            this.ingredientGrade = ingredientGrade;
            this.numOfOrder = numOfOrder;
            this.pricePerUnit = pricePerUnit;
            this.numReply   = numReply;
            this.orderStatus = orderStatus;
            this.utilityValue = utilityValue;
        }

        public String toStringOutput() {
        	String status;
        	if(this.orderStatus == 0) {
        		status = "Waiting queue";
        	}else if(this.orderStatus == 1){
				status = "Process";
			}else if(this.orderStatus == 2){
        	    status = "process some order";
            }
        	else{
        	    status = "Done";
            }
            return "Agent name: " + this.agentName + " Order name: " + this.orderName + "   Quality: " + this.ingredientGrade +
                    "   Order requested: " + this.numOfOrder + "  Price per unit: " +  this.pricePerUnit  + "  Order replied: " + this.numReply + "  Order status: " + status + "  Utility value: " + df.format(utilityValue);
        }
        public String toUpdateService(){

            return this.orderName + "-" + this.ingredientGrade + "-" + this.numOfOrder + "-" + df.format(this.pricePerUnit) + "-" + this.numReply + "-" + this.orderStatus + "-" + df.format(this.utilityValue);
        }
    }

    public class resultsetOnEachIngredient{
        public String ingredientName;
        public double gradARequire;
        public double gradBRequire;
        public double gradeCRequire;

        public resultsetOnEachIngredient(String ingredientName, double numRequree, double gradARequire, double gradeBRequire, double gradeCRequire){
            this.ingredientName = ingredientName;
            this.gradARequire = gradARequire;
            this.gradeCRequire = gradeCRequire;
        }
    }

    public class ingredientTable {
        public String ingredientName;
        public String ingredientGrade;
        public double numOfBefore;
        public double numOfAfter;
        public double percentage;


        public ingredientTable(String ingredientName, String ingredientGrade, double numOfBefore, double numOfAfter, double percentage) {
            this.ingredientName = ingredientName;
            this.ingredientGrade = ingredientGrade;
            this.numOfBefore = numOfBefore;
            this.numOfAfter = numOfAfter;
            this.percentage = percentage;
        }
        public String toString(){
            return String.format("Name : %s  Grade: %s Before: %.02f After: %.02f Pct: %.02f", this.ingredientName, this.ingredientGrade, this.numOfBefore, this.numOfAfter, this.percentage);
        }
    }
    public class orderTransaction{
        public int HamCheeseSandwichA_order, HamCheeseSandwichA_accept, HamCheeseSandwichB_order, HamCheeseSandwichB_accept, HamCheeseSandwichC_order, HamCheeseSandwichC_accept;
        public int HamSandwichA_order, HamSandwichA_accept, HamSandwichB_order, HamSandwichB_accept, HamSandwichC_order, HamSandwichC_accept;
        public int CheeseOnionA_order, CheeseOnionA_accept, CheeseOnionB_order, CheeseOnionB_accept, CheeseOnionC_order, CheeseOnionC_accept;
        public int CheesePickleA_order, CheesePickleA_accept, CheesePickleB_order, CheesePickleB_accept, CheesePickleC_order, CheesePickleC_accept;
        public int TunaA_order, TunaA_accept, TunaB_order, TunaB_accept, TunaC_order, TunaC_accept;

        public orderTransaction(int HamCheeseSandwichA_order, int HamCheeseSandwichA_accept, int HamCheeseSandwichB_order, int HamCheeseSandwichB_accept, int HamCheeseSandwichC_order, int HamCheeseSandwichC_accept,
        int HamSandwichA_order, int HamSandwichA_accept, int HamSandwichB_order, int HamSandwichB_accept, int HamSandwichC_order, int HamSandwichC_accept,
        int CheeseOnionA_order, int CheeseOnionA_accept, int CheeseOnionB_order, int CheeseOnionB_accept, int CheeseOnionC_order, int CheeseOnionC_accept,
        int CheesePickleA_order, int CheesePickleA_accept, int CheesePickleB_order, int CheesePickleB_accept, int CheesePickleC_order, int CheesePickleC_accept,
        int TunaA_order, int TunaA_accept, int TunaB_order, int TunaB_accept, int TunaC_order, int TunaC_accept){
            this.HamCheeseSandwichA_order = HamCheeseSandwichA_order;
            this.HamCheeseSandwichA_accept = HamCheeseSandwichA_accept;
            this.HamCheeseSandwichB_order = HamCheeseSandwichB_order;
            this.HamCheeseSandwichB_accept = HamCheeseSandwichB_accept;
            this.HamCheeseSandwichC_order = HamCheeseSandwichC_order;
            this.HamCheeseSandwichC_accept = HamCheeseSandwichC_accept;
            this.HamSandwichA_order = HamSandwichA_order;
            this.HamSandwichA_accept = HamSandwichA_accept;
            this.HamSandwichB_order = HamSandwichB_order;
            this.HamSandwichB_accept = HamSandwichB_accept;
            this.HamSandwichC_order = HamSandwichC_order;
            this.HamSandwichC_accept = HamSandwichC_accept;
            this.CheeseOnionA_order = CheeseOnionA_order;
            this.CheeseOnionA_accept = CheeseOnionA_accept;
            this.CheeseOnionB_order = CheeseOnionB_order;
            this.CheeseOnionB_accept = CheeseOnionB_accept;
            this.CheeseOnionC_order = CheeseOnionC_order;
            this.CheeseOnionC_accept = CheeseOnionC_accept;
            this.CheesePickleA_order = CheesePickleA_order;
            this.CheesePickleA_accept = CheesePickleA_accept;
            this.CheesePickleB_order = CheesePickleB_order;
            this.CheesePickleB_accept = CheesePickleB_accept;
            this.CheesePickleC_order = CheesePickleC_order;
            this.CheesePickleC_accept = CheesePickleC_accept;
            this.TunaA_order = TunaA_order;
            this.TunaA_accept = TunaA_accept;
            this.TunaB_order = TunaB_order;
            this.TunaB_accept = TunaB_accept;
            this.TunaC_order = TunaC_order;
            this.TunaC_accept = TunaC_accept;
        }
    }

    public class ingredientTransaction{
        public double WhiteBreadA, WhiteBreadA_after, WhiteBreadB, WhiteBreadB_after, WhiteBreadC, WhiteBreadC_after;
        public double HamA, HamA_after, HamB, HamB_after, HamC, HamC_after;
        public double MatureCheddarMilkA, MatureCheddarMilkA_after, MatureCheddarMilkB, MatureCheddarMilkB_after, MatureCheddarMilkC, MatureCheddarMilkC_after;
        public double OnionA, OnionA_after, OnionB, OnionB_after, OnionC, OnionC_after;
        public double PickleA, PickleA_after, PickleB, PickleB_after, PickleC, PickleC_after;
        public double TunaA, TunaA_after, TunaB, TunaB_after, TunaC, TunaC_after;
        public double SunflowerSpreadA, SunflowerSpreadA_after, SunflowerSpreadB, SunflowerSpreadB_after, SunflowerSpreadC, SunflowerSpreadC_after;

        public ingredientTransaction(double WhiteBreadA, double WhiteBreadA_after, double WhiteBreadB, double WhiteBreadB_after, double WhiteBreadC, double WhiteBreadC_after,
        double HamA, double HamA_after, double HamB, double HamB_after, double HamC, double HamC_after,
        double MatureCheddarMilkA, double MatureCheddarMilkA_after, double MatureCheddarMilkB, double MatureCheddarMilkB_after, double MatureCheddarMilkC, double MatureCheddarMilkC_after,
        double OnionA, double OnionA_after, double OnionB, double OnionB_after, double OnionC, double OnionC_after,
        double PickleA, double PickleA_after, double PickleB, double PickleB_after, double PickleC, double PickleC_after,
        double TunaA, double TunaA_after, double TunaB, double TunaB_after, double TunaC, double TunaC_after,
        double SunflowerSpreadA, double SunflowerSpreadA_after, double SunflowerSpreadB, double SunflowerSpreadB_after, double SunflowerSpreadC, double SunflowerSpreadC_after){
            this.WhiteBreadA = WhiteBreadA;
            this.WhiteBreadA_after = WhiteBreadA_after;
            this.WhiteBreadB = WhiteBreadB;
            this.WhiteBreadB_after = WhiteBreadB_after;
            this.WhiteBreadC = WhiteBreadC;
            this.WhiteBreadC_after = WhiteBreadC_after;
            this.HamA = HamA;
            this.HamA_after = HamA_after;
            this.HamB = HamB;
            this.HamB_after = HamB_after;
            this.HamC = HamC;
            this.HamC_after = HamC_after;
            this.MatureCheddarMilkA = MatureCheddarMilkA;
            this.MatureCheddarMilkA_after = MatureCheddarMilkA_after;
            this.MatureCheddarMilkB = MatureCheddarMilkB;
            this.MatureCheddarMilkB_after = MatureCheddarMilkB_after;
            this.MatureCheddarMilkC = MatureCheddarMilkC;
            this.MatureCheddarMilkC_after = MatureCheddarMilkC_after;
            this.OnionA = OnionA;
            this.OnionA_after = OnionA_after;
            this.OnionB = OnionB;
            this.OnionB_after = OnionB_after;
            this.OnionC = OnionC;
            this.OnionC_after = OnionC_after;
            this.PickleA = PickleA;
            this.PickleA_after = PickleA_after;
            this.PickleB = PickleB;
            this.PickleB_after = PickleB_after;
            this.PickleC = PickleC;
            this.PickleC_after = PickleC_after;
            this.TunaA = TunaA;
            this.TunaA_after = TunaA_after;
            this.TunaB = TunaB;
            this.TunaB_after = TunaB_after;
            this.TunaC = TunaC;
            this.TunaC_after = TunaC_after;
            this.SunflowerSpreadA = SunflowerSpreadA;
            this.SunflowerSpreadA_after = SunflowerSpreadA_after;
            this.SunflowerSpreadB = SunflowerSpreadB;
            this.SunflowerSpreadB_after = SunflowerSpreadB_after;
            this.SunflowerSpreadC = SunflowerSpreadC;
            this.SunflowerSpreadC_after = SunflowerSpreadC_after;
        }

        public List nameIndex(){
            List resultList = Arrays.asList("WhiteBreadA_after","WhiteBreadB_after","WhiteBreadC_after", "HamA_after","HamB_after","HamC_after","MatureCheddarMilkA_after",
                    "MatureCheddarMilkB_after","MatureCheddarMilkC_after","OnionA_after","OnionB_after","OnionC_after","PickleA_after","PickleB_after","PickleC_after",
                    "TunaA_after","TunaB_after","TunaC_after","SunflowerSpreadA_after","SunflowerSpreadB_after","SunflowerSpreadC_after");
            return resultList;
        }
    }
}
