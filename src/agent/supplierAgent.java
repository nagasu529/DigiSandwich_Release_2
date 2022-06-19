package agent;

import database.DatabaseConn;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;

public class supplierAgent extends Agent {
    supplierGui myGui;
    DecimalFormat df = new DecimalFormat("#.##");
    DatabaseConn app = new DatabaseConn();
    calcMethod calc = new calcMethod();

    // The catalogue of supply items (maps the title name to its quantities)
    ArrayList<supplierInfo> sellingProductList = new ArrayList<>();
    ArrayList<weeklyReport> stockOfIngredients = new ArrayList<>();
    ArrayList<weeklyReport> requestFromSpecialist = new ArrayList<>();
    ArrayList<weeklyReport> refillStockList = new ArrayList<>();

    int dayTimer = 15000;
    int dayCount = 0;
    int weekCount = 1;

    double maxStockCapacity = 1000000;

    String supplierStock = "med-spikeUp30D10-stdOverSpecialist-stdSupply-supplierStock";
    String ingredientReq = "med-spikeUp30D10-stdOverSpecialist-stdSupply-ingredientReq";
    String refillStock = "med-spikeUp30D10-stdOverSpecialist-stdSupply-refillStock";

    //int[] orderTimerArray = {40000,70000};

    // The GUI by means of which the user can add books in the catalogue
    //public supplierUI myGui;

    //Home PC classpath
    String supplierStockClasspath = String.format("C:\\Users\\Krist\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",supplierStock);
    String ingredientReqClasspath = String.format("C:\\Users\\Krist\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",ingredientReq);
    String refillStockClasspath = String.format("C:\\Users\\Krist\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",refillStock);

    //NB Office classpath
    //String supplierStockClasspath = String.format("C:\\Users\\KChiewchanadmin\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",supplierStock);
    //String ingredientReqClasspath = String.format("C:\\Users\\KChiewchanadmin\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",ingredientReq);
    //String refillStockClasspath = String.format("C:\\Users\\KChiewchanadmin\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",refillStock);

    //PC Office classpath
    //String supplierStockClasspath = String.format("C:\\Users\\kitti\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",supplierStock);
    //String ingredientReqClasspath = String.format("C:\\Users\\kitti\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",ingredientReq);
    //String refillStockClasspath = String.format("C:\\Users\\kitti\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",refillStock);

    //OSX classpath
    //String supplierStockClasspath = String.format("/Users/nagasu/IdeaProjects/DigiSandwich_Release_2/output/%s.csv",supplierStock);
    //String ingredientReqClasspath = String.format("/Users/nagasu/IdeaProjects/DigiSandwich_Release_2/output/%s.csv",ingredientReq);
    //String refillStockClasspath = String.format("/Users/nagasu/IdeaProjects/DigiSandwich_Release_2/output/%s.csv",refillStock);


    //Request from specialist classpath
    protected void setup() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //create CSV files (ingradStockupdate and ingredReq).
        try {
            String[] supplierStockIndex = {"Week","WhiteBread","Ham","Onion","Pickle","Tuna","Spread"};
            calc.createCSV(supplierStockClasspath,supplierStockIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String[] ingredientReqIndex = {"Week","WhiteBread","Ham","Onion","Pickle","Tuna","Spread"};
            calc.createCSV(ingredientReqClasspath,ingredientReqIndex);
        }catch (IOException e){
            e.printStackTrace();
        }

        try {
            String[] refillStockIdx = {"Week","WhiteBread","Ham","Onion","Pickle","Tuna","Spread"};
            calc.createCSV(refillStockClasspath,refillStockIdx);
        }catch (IOException e){
            e.printStackTrace();
        }

        // Create and show the GUI
            //myGui = new supplierUI(this);
            //myGui.showGui();
        myGui = new supplierGui(this);
        myGui.show();
            // Register the book-selling service in the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("supply-selling");
            sd.setName(getAID().getLocalName());
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }

        //prepaired nextWeekIngrad
        addBehaviour(new receivingSupplyRequest());

        //First week intialise for Raynor's stock
        stockOfIngredients.add(new weeklyReport(weekCount,maxStockCapacity,maxStockCapacity,maxStockCapacity,maxStockCapacity,maxStockCapacity,maxStockCapacity));

        sellingProductList.add(new supplierInfo(getLocalName(),"WhiteBread","general",0));
        sellingProductList.add(new supplierInfo(getLocalName(),"Ham","general",0));
        sellingProductList.add(new supplierInfo(getLocalName(),"Spread","general",0));

        /*
        sellingProductList.add(new supplierInfo(getLocalName(),"WhiteBread","general",maxStockCapacity));
        sellingProductList.add(new supplierInfo(getLocalName(),"Ham","general",maxStockCapacity));
        sellingProductList.add(new supplierInfo(getLocalName(),"Spread","general",maxStockCapacity));

        //Adding current update ingredient to lastWeekDeliverStock (First week). Then, clearing the status on sellingPorduct list to zero and waiting for incoming ingredients requirement.
        stockOfIngredients.add(new weeklyReport(weekCount,0,0,0,0,0,0));
        for(int i = 0; i < sellingProductList.size();i++){
            String productName = sellingProductList.get(i).productName;
            switch (productName){
                case "WhiteBread":
                    stockOfIngredients.get(0).WhiteBread = sellingProductList.get(i).numOfstock;
                    //updateProduct(getLocalName(),productName,sellingProductList.get(i).ingredientGrade,sellingProductList.get(i).numOfstock);
                    sellingProductList.get(i).numOfstock = 0;
                    break;
                case "Ham":
                    stockOfIngredients.get(0).Ham = sellingProductList.get(i).numOfstock;
                    //updateProduct(getLocalName(),productName,sellingProductList.get(i).ingredientGrade,sellingProductList.get(i).numOfstock);
                    sellingProductList.get(i).numOfstock = 0;
                    break;
                case "Spread":
                    stockOfIngredients.get(0).Spread = sellingProductList.get(i).numOfstock;
                    //updateProduct(getLocalName(),productName,sellingProductList.get(i).ingredientGrade,sellingProductList.get(i).numOfstock);
                    sellingProductList.get(i).numOfstock = 0;
                    break;
            }
        }
        */

        //Writing to initialize stock at first week
        try {
            calc.updateCSVFile(supplierStockClasspath,stockOfIngredients.get(0).rowData());
            //myGui.displayUI(stockOfIngredients.get(0).toStringDisplay());
        }catch (IOException e){
            e.printStackTrace();
        }

        //Add a TickerBehaviour to refill supply (1 time a week).
        addBehaviour(new TickerBehaviour(this, dayTimer){
            protected void onTick() {
                //Day count added.
                dayCount++;
                String day = calc.dayInWeek(dayCount);
                //Checking the weekly list order.
                if(day == "Monday" && dayCount > 6){
                    weekCount++;
                    //Initialize for current week stock writing.
                    stockOfIngredients.add(new weeklyReport(weekCount,0,0,0,0,0,0));
                    for (int i = 0; i < sellingProductList.size();i++){
                        System.out.println(String.format("Current stage of sellingProductList on  %s    value:   %.02f",sellingProductList.get(i).productName, sellingProductList.get(i).numOfstock));

                        //Checking ingredient delivery request.
                        String productName = sellingProductList.get(i).productName;
                        switch (productName){
                            case "WhiteBread":
                                /*
                                //Normal case
                                stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread = stockOfIngredients.get(stockOfIngredients.size() - 2).WhiteBread - sellingProductList.get(i).numOfstock;
                                if(stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread < 0){
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 2).WhiteBread);
                                    stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread = 0;
                                }else {
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread);

                                }
                                sellingProductList.get(i).numOfstock = 0;

                                 */


                                //Normal case
                                stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread = stockOfIngredients.get(stockOfIngredients.size() - 2).WhiteBread - requestFromSpecialist.get(requestFromSpecialist.size() - 1).WhiteBread;
                                if(stockOfIngredients.get(stockOfIngredients.size() -1).WhiteBread < 0){
                                    stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread = 0;

                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 2).WhiteBread);
                                }else{
                                    updateProduct(getLocalName(),productName,sellingProductList.get(i).ingredientGrade, requestFromSpecialist.get(requestFromSpecialist.size() - 1).WhiteBread);
                                }
                                break;
                            case "Ham":
                                /*
                                //Normal case
                                stockOfIngredients.get(stockOfIngredients.size() - 1).Ham = stockOfIngredients.get(stockOfIngredients.size() - 2).Ham - sellingProductList.get(i).numOfstock;
                                if(stockOfIngredients.get(stockOfIngredients.size() - 1).Ham < 0){
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 2).Ham);
                                    stockOfIngredients.get(stockOfIngredients.size() - 1).Ham = 0;
                                }else {
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 1).Ham);
                                }
                                sellingProductList.get(i).numOfstock = 0;

                                //Don't case about supplier stock
                                stockOfIngredients.get(stockOfIngredients.size() - 1).Ham = stockOfIngredients.get(stockOfIngredients.size() - 2).Ham - sellingProductList.get(i).numOfstock;
                                if(sellingProductList.get(i).numOfstock > 0){
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
                                }
                                //sellingProductList.get(i).numOfstock = 0;

                                 */
                                //Normal case
                                stockOfIngredients.get(stockOfIngredients.size() - 1).Ham = stockOfIngredients.get(stockOfIngredients.size() - 2).Ham - requestFromSpecialist.get(requestFromSpecialist.size() - 1).Ham;
                                if(stockOfIngredients.get(stockOfIngredients.size() -1).Ham < 0){
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 2).Ham);
                                    stockOfIngredients.get(stockOfIngredients.size() - 1).Ham = 0;
                                }else{
                                    updateProduct(getLocalName(),productName,sellingProductList.get(i).ingredientGrade, requestFromSpecialist.get(requestFromSpecialist.size() - 1).Ham);
                                }
                                break;


                            case "Spread":
                                /*
                                //Normal case
                                stockOfIngredients.get(stockOfIngredients.size() - 1).Spread = stockOfIngredients.get(stockOfIngredients.size() - 2).Spread - sellingProductList.get(i).numOfstock;
                                if(stockOfIngredients.get(stockOfIngredients.size() - 1).Spread < 0){
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 2).Spread);
                                    stockOfIngredients.get(stockOfIngredients.size() - 1).Spread = 0;

                                }else {
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 1).Spread);
                                }
                                sellingProductList.get(i).numOfstock = 0;

                                //Don't case about supplier stock
                                stockOfIngredients.get(stockOfIngredients.size() - 1).Spread = stockOfIngredients.get(stockOfIngredients.size() - 2).Spread - sellingProductList.get(i).numOfstock;
                                if(sellingProductList.get(i).numOfstock > 0){
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
                                }
                                 */
                                //Normal case
                                stockOfIngredients.get(stockOfIngredients.size() - 1).Spread = stockOfIngredients.get(stockOfIngredients.size() - 2).Spread - requestFromSpecialist.get(requestFromSpecialist.size() - 1).Spread;
                                if(stockOfIngredients.get(stockOfIngredients.size() -1).Spread < 0){
                                    stockOfIngredients.get(stockOfIngredients.size() - 1).Spread = 0;
                                    updateProduct(getLocalName(),productName, sellingProductList.get(i).ingredientGrade, stockOfIngredients.get(stockOfIngredients.size() - 2).Spread);
                                }else{
                                    updateProduct(getLocalName(),productName,sellingProductList.get(i).ingredientGrade, requestFromSpecialist.get(requestFromSpecialist.size() - 1).Spread);
                                }
                                break;
                        }
                    }
                    //Writing to stockUpdate for each week to CSV
                    try {
                        calc.updateCSVFile(supplierStockClasspath,stockOfIngredients.get(stockOfIngredients.size() - 1).rowData());
                        myGui.displayUI(stockOfIngredients.get(stockOfIngredients.size() - 1).toStringDisplay());
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }

                if(day == "Tuesday" && dayCount > 6){
                    addBehaviour(new refilledStock());
                }

                if(day == "Saturday"){
                    System.out.println(" num of row:   " + requestFromSpecialist.size());
                    requestFromSpecialist.add(new weeklyReport(weekCount,0,0,0,0,0,0));
                    int lastIndex = requestFromSpecialist.size() - 1;
                    for(int i = 0; i < sellingProductList.size();i++){
                        String productName = sellingProductList.get(i).productName;
                        switch (productName){
                            case "WhiteBread":
                                requestFromSpecialist.get(lastIndex).WhiteBread = sellingProductList.get(i).numOfstock;
                                sellingProductList.get(i).numOfstock = 0;
                                break;
                            case "Ham":
                                requestFromSpecialist.get(lastIndex).Ham = sellingProductList.get(i).numOfstock;
                                sellingProductList.get(i).numOfstock = 0;
                                break;
                            case "Spread":
                                requestFromSpecialist.get(lastIndex).Spread = sellingProductList.get(i).numOfstock;
                                sellingProductList.get(i).numOfstock = 0;
                                break;
                        }
                    }
                    try {
                        calc.updateCSVFile(ingredientReqClasspath, requestFromSpecialist.get(requestFromSpecialist.size() - 1).rowData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } );
            //doSuspend();
            //myGui.dispose();
            
            // Add the behaviour serving purchase orders from buyer agents
            //addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        //System.out.println("Seller-agent "+getAID().getName()+" terminating.");
        // Close the GUI
        //myGui.dispose();
    }

    /**
     This is invoked by the GUI when the supplier agent update stock.
     */
    public void updateProduct(final String agentName, final String productName, final String ingredientGrade, final double quantity) {
        addBehaviour(new OneShotBehaviour() {
            private AID[] specialistAgent;
            public void action() {
                //fake adding date to stock.
                //LocalDate AddedToStock = java.time.LocalDate.of(2021, 9,supplierInfo.getRandIntRange(13,15));
                LocalDate AddedToStock = java.time.LocalDate.now().plusDays(dayCount);

                //Getting data from GUI.
                calcMethod supplierInfo = new calcMethod();
                calcMethod.supplierInfo input = supplierInfo.new supplierInfo(agentName,productName,ingredientGrade,quantity,AddedToStock);

                //Searching specialist agent and created address table.
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sdSearch = new ServiceDescription();
                sdSearch.setType("specialist");
                template.addServices(sdSearch);
                try {
                    DFAgentDescription[] searchResult = DFService.search(myAgent, template);
                    specialistAgent = new AID[searchResult.length];
                    for (int j = 0; j < searchResult.length; ++j) {
                        specialistAgent[j] = searchResult[j].getName();
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
                //Sending updated service to all specialists.
                ACLMessage serviceSender = new ACLMessage(ACLMessage.PROPOSE);
                for (int j = 0; j < specialistAgent.length; ++j) {
                    serviceSender.addReceiver(specialistAgent[j]);
                }
                //Updating the ingredient stock.
                //sellingProductList.add(input);
                serviceSender.setContent(input.toUpdateService());
                serviceSender.setConversationId("Supplier");
                myAgent.send(serviceSender);

                System.out.println(" \n Service sender from supplier:     " + serviceSender);

            }
        } );
    }

    private class receivingSupplyRequest extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null && msg.getConversationId().equals("Supplier")){
                System.out.println(" \n Request receiving:  " + msg);
                String[] arrOfStr = msg.getContent().split("-");
                String tempName = arrOfStr[0];
                double tempNumRequested = Double.parseDouble(arrOfStr[1]);
                for(int i = 0; i < sellingProductList.size();i++){
                    if(tempName.equals(sellingProductList.get(i).productName)){
                        sellingProductList.get(i).numOfstock = tempNumRequested;
                        //sellingProductList.get(i).status = 1;
                    }
                }
                //Checking next week delivery before sending proposed ACCEPT_PROPOSAL

                //Sending the ACCEPT message to confirm next week order.
                ACLMessage replyMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                replyMsg.setConversationId("reply-to-Specialist");
                replyMsg.addReceiver(msg.getSender());
                //System.out.print("Message from suppliers:  " + replyMsg);

            }else {
                block();
            }
        }
    }
    //Agent state for order refill stock if needed.
    private class refilledStock extends OneShotBehaviour {
        public void action(){
            //Initialize
            int percentage = 0;
            int windowSize = 2;

            //Standard Optimization
            double breadNeed = standardOptMethod(percentage,"WhiteBread",stockOfIngredients);
            double hamNeed = standardOptMethod(percentage,"Ham",stockOfIngredients);
            double spreadNeed = standardOptMethod(percentage,"Spread",stockOfIngredients);

            //SMA optimization
            //double breadNeed = smaOptMethod(percentage,windowSize,"WhiteBread",refillStockList,stockOfIngredients);
            //double hamNeed = smaOptMethod(percentage,windowSize,"Ham",refillStockList,stockOfIngredients);
            //double spreadNeed = smaOptMethod(percentage,windowSize,"Spread",refillStockList,stockOfIngredients);

            //refill stock to refrigerator
            stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread = stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread + breadNeed;
            stockOfIngredients.get(stockOfIngredients.size() - 1).Ham = stockOfIngredients.get(stockOfIngredients.size() -1).Ham + hamNeed;
            stockOfIngredients.get(stockOfIngredients.size() - 1).Spread = stockOfIngredients.get(stockOfIngredients.size() - 1).Spread + spreadNeed;
            //adding new row to list.
            refillStockList.add(new weeklyReport(weekCount,breadNeed,hamNeed,0,0,0,spreadNeed));

            //CSV writing.
            try {
                calc.updateCSVFile(refillStockClasspath, refillStockList.get(refillStockList.size() - 1).rowData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class replyToSpecialist extends OneShotBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null && msg.getConversationId().equals("Specialist")){
                String[] arrOfStr = msg.getContent().split("-");
                String tempName = arrOfStr[0];
                int tempNumRequested = Integer.parseInt(arrOfStr[2]);
                int localChange = sellingProductList.indexOf(tempName);
                sellingProductList.get(localChange).numOfstock = tempNumRequested;
            }else {
                block();
            }
        }
    }

    //Order prediction method that is applied for all agent types.
    private double standardOptMethod (int percentage, String ingredName, ArrayList<supplierAgent.weeklyReport> weeklyResult){
        double result = 0;
        int historyRecord = weeklyResult.size();
        if(historyRecord <= 1){
            result = 0;
        }else {
            switch (ingredName){
                case "WhiteBread":
                    double breadLastWeek = weeklyResult.get(weeklyResult.size() - 2).WhiteBread - weeklyResult.get(weeklyResult.size() - 1).WhiteBread;
                    if(weeklyResult.get(weeklyResult.size() - 1).WhiteBread > (breadLastWeek * 2)){
                        result = 0;
                    }else {
                        result = breadLastWeek + (breadLastWeek * (percentage/100));
                    }
                    break;

                case "Ham":
                    double hamLastWeek = weeklyResult.get(weeklyResult.size() - 2).Ham - weeklyResult.get(weeklyResult.size() - 1).Ham;
                    if(weeklyResult.get(weeklyResult.size() - 1).Ham > (hamLastWeek * 2)){
                        result = 0;
                    }else {
                        result = hamLastWeek + (hamLastWeek * (percentage/100));
                                /*
                                if(result > maxStockHam){
                                    result = maxStockHam - weeklyResult.get(weeklyResult.size() - 1).Ham;
                                }

                                 */
                    }
                    break;

                case "Spread":
                    double spreadLastWeek = weeklyResult.get(weeklyResult.size() - 2).Spread - weeklyResult.get(weeklyResult.size() - 1).Spread;
                    if(weeklyResult.get(weeklyResult.size() - 1).Ham > (spreadLastWeek * 2)){
                        result = 0;
                    }else {
                        result = spreadLastWeek + (spreadLastWeek * (percentage/100));
                                /*
                                if(result > maxStockSpread){
                                    result = maxStockSpread - weeklyResult.get(weeklyResult.size() -1).Spread;
                                }
                                 */
                    }
                    break;
            }
        }
        System.out.println(String.format("Standard method (Supplier stock refill)           %.2f",result));
        return result;
    }

    private double smaOptMethod (int percentage, int windowSize, String ingredName, ArrayList<supplierAgent.weeklyReport> refillStockList, ArrayList<supplierAgent.weeklyReport> stockOfIngredients){
        double result = 0;
        String tmpText = "";
        //Checking window size for SMA calculation.
        //We would average base on difference window size when ArrayList size is less than window size value.

        //System.out.println(String.format(" RefillStockList size:       %d                 %s",refillStockList.size(), refillStockList.get(refillStockList.size() - 1).toString()));
        if(refillStockList.size() == 0 || windowSize == 0){
            result = 0;
        } else if (refillStockList.size() > 0 && refillStockList.size() < windowSize) {
            for (int i = 0; i < refillStockList.size(); i++){
                switch (ingredName){
                    case "WhiteBread":
                        tmpText = "WhiteBread";
                        result = stockOfIngredients.get(stockOfIngredients.size() - 2).WhiteBread - stockOfIngredients.get(stockOfIngredients.size() - 1).WhiteBread;
                        break;
                    case "Ham":
                        tmpText = "Ham";
                        //result = refillStockList.get(refillStockList.size() - 1).Ham;
                        result = stockOfIngredients.get(stockOfIngredients.size() - 2).Ham - stockOfIngredients.get(stockOfIngredients.size() - 1).Ham;

                        break;
                    case "Spread":
                        tmpText = "Spread";
                        //result = refillStockList.get(refillStockList.size()  - 1).Spread;
                        result = stockOfIngredients.get(stockOfIngredients.size() - 2).Spread - stockOfIngredients.get(stockOfIngredients.size() - 1).Spread;
                        break;
                }
            }
        } else {
            switch (ingredName){
                case "WhiteBread":
                    tmpText = "WhiteBread";
                    int breadSize = 0;
                    while (breadSize != windowSize){
                        result = result + refillStockList.get(refillStockList.size() - 1 - breadSize).WhiteBread;
                        breadSize++;
                    }
                    result = result/windowSize;
                    break;
                case "Ham":
                    tmpText = "Ham";
                    int hamSize = 0;
                    while (hamSize != windowSize){
                        result = result + refillStockList.get(refillStockList.size() - 1 - hamSize).Ham;
                        hamSize++;
                    }
                    result = result/windowSize;
                    break;
                case "Spread":
                    tmpText = "Spread";
                    int spreadSize = 0;
                    while (spreadSize != windowSize){
                        result = result + refillStockList.get(refillStockList.size() - 1 - spreadSize).Spread;
                        spreadSize++;
                    }
                    result = result/windowSize;
                    break;
                }
        }
        return result;
    }

    /**
     Inner class PurchaseOrdersServer.
     This is the behaviour used by Book-seller agents to serve incoming
     offer acceptances (i.e. purchase orders) from buyer agents.
     The seller agent removes the purchased book from its catalogue
     and replies with an INFORM message to notify the buyer that the
     purchase has been sucesfully completed.
     */

    private class supplierInfo{
        public String agentName;
        public String productName;
        public String ingredientGrade;
        public double numOfstock;

        public supplierInfo(String agentName, String productName, String ingredientGrade, double numOfstock) {
            this.agentName = agentName;
            this.productName = productName;
            this.ingredientGrade = ingredientGrade;
            this.numOfstock = numOfstock;
        }

        public String toStringOutput() {
            return "Agent name: "+ this.agentName + " Ingredient name: " + this.productName + "   Quality: " + this.ingredientGrade + "   Stock: " + df.format(numOfstock);
        }
    }

    private class weeklyReport{
        public int Week;
        public double WhiteBread;
        public double Ham;
        public double Onion;
        public double Pickle;
        public double Tuna;
        public double Spread;

        public weeklyReport(int Week, double WhiteBread, double Ham, double Onion, double Pickle, double Tuna, double Spread){
            this.Week = Week;
            this.WhiteBread = WhiteBread;
            this.Ham = Ham;
            this.Onion = Onion;
            this.Pickle = Pickle;
            this.Tuna = Tuna;
            this.Spread = Spread;
        }
        public String toString(){
            return String.format("WB: %.2f    Ham: %.2f    Spread: %.2f ",this.WhiteBread, this.Ham, this.Spread);
        }
        public String[] indexCSV(){
            String[] resultIndex = {"Week","WhiteBread","Ham","Onion","Pickle","Tuna","Spread"};
            return resultIndex;
        }
        public String[] rowData(){
            String[] result = {String.valueOf(this.Week),String.valueOf(this.WhiteBread),String.valueOf(this.Ham),String.valueOf(this.Onion),
                    String.valueOf(this.Pickle),String.valueOf(this.Tuna),String.valueOf(this.Spread)};
            return result;
        }
        public String toStringDisplay(){
            String weeklyDisply = String.format("Current stock (Weekly): \n" +
                    "Whitebread : %.2f  Ham: %.2f  Spread: %.2f \n",this.WhiteBread,this.Ham, this.Spread);
            return weeklyDisply;
        }
    }
}

