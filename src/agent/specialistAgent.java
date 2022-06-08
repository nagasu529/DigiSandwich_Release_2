package agent;

import database.DatabaseConn;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;


public class specialistAgent extends Agent {
    private specialistGui myGui;

    //Initial Data table and others parameters.

    ArrayList<calcMethod.supplierInfo> supplierDataList = new ArrayList<>();   //List of available ingredient
    ArrayList<calcMethod.customerInfo> customerDataList = new ArrayList<>();   //List of request orders.

    ArrayList<weeklyResult> weeklyResult = new ArrayList<>();                  //The data collection for weekly report.
    ArrayList<ingredientTransaction> dailyTransaction = new ArrayList<>();

    calcMethod calcMethod = new calcMethod();
    DatabaseConn app = new DatabaseConn();

    //Initialize value befor calculation
    String dailyName = "med-spikeUp30D10-stdOverSpecialist-stdSupply-dailyResult";
    String weeklyName = "med-spikeUp30D10-stdOverSpecialist-stdSupply-weeklyResult";

    //Initial order value stage.
    double numOfIngradforProduct = 7000;        //Number of product for 2 weeks

    double numBread = app.selectQuantity("HamSandwich","WhiteBread") * numOfIngradforProduct;
    double numHam = app.selectQuantity("HamSandwich","Ham") * numOfIngradforProduct;
    double numSpread = app.selectQuantity("HamSandwich","Spread") * numOfIngradforProduct;

    int dayTimer = 15000;
    int dayTimeCount = 0;

    //Create CSV classpath.
    //Home PC classpath.
    //String dailyResult = String.format("C:\\Users\\Krist\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",dailyName);
    //String weeklyResultPath = String.format("C:\\Users\\Krist\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",weeklyName);

    //PC Office classpath.
    //String dailyResult = String.format("C:\\Users\\kitti\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",dailyName);
    //String weeklyResultPath = String.format("C:\\Users\\kitti\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",weeklyName);

    //NB office classpath.
    //String dailyResult = String.format("C:\\Users\\KChiewchanadmin\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",dailyName);
    //String weeklyResultPath = String.format("C:\\Users\\KChiewchanadmin\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",weeklyName);

    //OSX classpath.
    String dailyResult =String.format("/Users/nagasu/IdeaProjects/DigiSandwich_Release_2/output/%s.csv",dailyName);
    String weeklyResultPath = String.format("/Users/nagasu/IdeaProjects/DigiSandwich_Release_2/output/%s.csv",weeklyName);

    String[] entry = {"totalPaticipant", "totalOrder", "totalOrderAccept","totalOrderReject", "WB", "WB_after", "Ham", "Ham_after", "Onion", "Onion_after", "Pickle", "Pickle_after", "Tuna", "Tuna_after", "Spread", "Spread_after"};


    //Updating agent services
    protected void setup() {
        //Gui active.
        // Create and show the GUI
        //myGui = new biddingSpecialistUI(this);
        //myGui.show();

        //Initialize ingredient supply in stock that are coverred for two weeks.
        LocalDate AddedToStock = java.time.LocalDate.now().minusDays(7);
        supplierDataList.add(calcMethod.new supplierInfo("Initial","WhiteBread","general",numBread,AddedToStock));
        supplierDataList.add(calcMethod.new supplierInfo("Initial","Ham","general",numHam,AddedToStock));
        supplierDataList.add(calcMethod.new supplierInfo("Initial","Spread","general",numSpread,AddedToStock));

        try {
            calcMethod.createCSV(dailyResult,entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
        weeklyResult.add(new weeklyResult("HamSandwich",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0));
        //Prepairing the CSV with header (Weekly report)
        try {
            String[] entryWeekly = weeklyResult.get(0).indexCSV();
            calcMethod.createCSV(weeklyResultPath,entryWeekly);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("specialist");
        sd.setName(getAID().getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        myGui = new specialistGui(this);
        myGui.show();

        addBehaviour(new specialistAgent.updateStockFromSupplier());
        addBehaviour(new specialistAgent.receiveOrderFromCustomer());
        
        //The service reply process which is after the end of auction. Agent 

        //Add a TickerBehaviour that shows the list of ingredients stock and order queue.
        addBehaviour(new TickerBehaviour(this, dayTimer){
            protected void onTick() {
                //update current stock on the list of suppliers.
                for(int i = 0; i < supplierDataList.size(); i++) {
                    if(supplierDataList.get(i).numOfstock <=0){
                        supplierDataList.remove(i);
                    }
                }
                /** 
                if(supplierDataList.size() != 0){
                   //myGui.displayUI("");
                    //myGui.displayUI(">>>>> Ingredient list <<<<<");
                    for(calcMethod.supplierInfo a : supplierDataList){
                        //myGui.displayUI(a.toStringOutput());
                    }
                }
                */

                //adding time to finish
                if(dayTimeCount < 65){
                    dayTimeCount++;
                    addBehaviour(new optimizeOrderFromcurrentStock());

                    String day = calcMethod.dayInWeek(dayTimeCount);
                    System.out.println(String.format("Day %d is %s",dayTimeCount, day));

                    if(day.equals("Saturday")){
                        addBehaviour(new nextWeekIngradReq());
                    }

                    //Weekly writing for Supplier request.
                    if(day.equals("Sunday")){
                        //Writing weekly total all incoming.
                        System.out.println("Staring to update the weekly file");

                        //latest weekly data in ArraList index.
                        int latestIndex = weeklyResult.size() - 1;

                        ArrayList<String> queryResult = app.selectProduct("HamSandwich");
                        for(int i = 0; i < queryResult.size();i++) {
                            if (queryResult.get(i) != null) {
                                double numPerOneProduct = app.selectQuantity("HamSandwich", queryResult.get(i));
                                switch (queryResult.get(i)){
                                    case "WhiteBread":
                                        weeklyResult.get(latestIndex).WhiteBreadNeed = numPerOneProduct * weeklyResult.get(latestIndex).numOfOrder;
                                        for(int j = 0; j < supplierDataList.size();j++){
                                            if(supplierDataList.get(j).productName.equals("WhiteBread")){
                                                weeklyResult.get(latestIndex).WhiteBread = weeklyResult.get(latestIndex).WhiteBread + supplierDataList.get(j).numOfstock;
                                            }
                                        }
                                        break;
                                    case "Ham":
                                        weeklyResult.get(latestIndex).HamNeed = numPerOneProduct * weeklyResult.get(latestIndex).numOfOrder;
                                        for(int j = 0; j < supplierDataList.size();j++){
                                            if(supplierDataList.get(j).productName.equals("Ham")){
                                                weeklyResult.get(latestIndex).Ham = weeklyResult.get(latestIndex).Ham + supplierDataList.get(j).numOfstock;
                                            }
                                        }
                                        break;
                                    case "Spread":
                                        weeklyResult.get(latestIndex).SpreadNeed = numPerOneProduct * weeklyResult.get(latestIndex).numOfOrder;
                                        for(int j = 0; j < supplierDataList.size();j++){
                                            if(supplierDataList.get(j).productName.equals("Spread")){
                                                weeklyResult.get(latestIndex).Spread = weeklyResult.get(latestIndex).Spread + supplierDataList.get(j).numOfstock;
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                        //sending the next week request here.
                        //ddBehaviour(new nextWeekIngradReq());

                        //writing data in row.
                        try {
                            calcMethod.updateCSVFile(weeklyResultPath,weeklyResult.get(latestIndex).rowData());
                            myGui.displayUI("Weekly summary: " + weeklyResult.get(latestIndex).toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //Clear weekly data and creating the new row.
                        //weeklyResult.clear();
                        weeklyResult.add(new weeklyResult("HamSandwich",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0));
                        //reset countTick
                        //weekCountTick = 0;
                    }
                    /*
                    tempdayOfweek++;
                    //weekly counter.
                    if(tempdayOfweek == 7){
                        //Active weekly count tick to 1.
                        weekCountTick = 1;
                        //reset the tempdayOfweek
                        tempdayOfweek = 0;
                    }
                     */
                }else{
                    //Writting the current ingredient stock before environment is terminated.

                    //Shutdown JADE Environment.
                    Codec codec = new SLCodec();
                    Ontology jmo = JADEManagementOntology.getInstance();
                    getContentManager().registerLanguage(codec);
                    getContentManager().registerOntology(jmo);
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(getAMS());
                    msg.setLanguage(codec.getName());
                    msg.setOntology(jmo.getName());
                    try {
                        getContentManager().fillContent(msg, new Action(getAID(), new ShutdownPlatform()));
                        send(msg);
                    }catch (Exception e) {}
                    myAgent.doSuspend();
                }
            }
        } );
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
        // Close the GUI
        //myGui.dispose();
        // Printout a dismissal message
        //System.out.println("Specialist-agent "+getAID().getName()+" terminating.");
    }

    //Sending the Ingredients request to suppliers base on week n needs in total.
    private class nextWeekIngradReq extends OneShotBehaviour{
        private AID[] supplierAgent;
        public void action(){
            int overEstPct = 0;
            int windowSize = 2;

            //Searching specialist agent and created address table.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdSearch = new ServiceDescription();
            sdSearch.setType("supply-selling");
            template.addServices(sdSearch);
            try {
                DFAgentDescription[] searchResult = DFService.search(myAgent, template);
                supplierAgent = new AID[searchResult.length];
                for (int i = 0; i < searchResult.length; ++i) {
                    supplierAgent[i] = searchResult[i].getName();
                    System.out.println("supplier Name: " + supplierAgent[i].getName());
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }
            //Message prepairation for all suppliers.
            ACLMessage serviceSender = new ACLMessage(ACLMessage.PROPOSE);
            for (int i = 0; i < supplierAgent.length; ++i) {
                serviceSender.addReceiver(supplierAgent[i]);
            }

            double breadNeed = standardOptimzation(overEstPct,"WhiteBread", weeklyResult);
            //double breadNeed = smaOptimaization(windowSize,overEstPct,"WhiteBread",weeklyResult);
            serviceSender.setContent(String.format("WhiteBread-%.2f",breadNeed));
            serviceSender.setConversationId("Supplier");
            myAgent.send(serviceSender);

            double hamNeed = standardOptimzation(overEstPct,"Ham", weeklyResult);
            //double hamNeed = smaOptimaization(windowSize,overEstPct,"Ham",weeklyResult);
            serviceSender.setContent(String.format("Ham-%.2f",hamNeed));
            serviceSender.setConversationId("Supplier");
            myAgent.send(serviceSender);

            double spreadNeed = standardOptimzation(overEstPct,"Spread",weeklyResult);
            //double spreadNeed = smaOptimaization(windowSize,overEstPct,"Spread",weeklyResult);
            serviceSender.setContent(String.format("Spread-%.2f",spreadNeed));
            serviceSender.setConversationId("Supplier");
            myAgent.send(serviceSender);

            /*
            //Weekly ingredients need calculation.
            double breadNeed = weeklyResult.get(weeklyResult.size() - 1).WhiteBreadNeed;
            double hamNeed = weeklyResult.get(weeklyResult.size() - 1).HamNeed;
            double spreadNeed = weeklyResult.get(weeklyResult.size() - 1).SpreadNeed;

            if(weeklyResult.get(weeklyResult.size() - 1).WhiteBreadNeed - dailyTransaction.get(0).WhiteBread_after > 0){
                breadNeed = breadNeed - dailyTransaction.get(0).WhiteBread_after;
                serviceSender.setContent("WhiteBread" + "-" + breadNeed);
                serviceSender.setConversationId("Supplier");
                myAgent.send(serviceSender);
            }

            int totalWeekly = weeklyResult.get(weeklyResult.size() - 1).numOfOrder;
            ArrayList<String> queryResult = app.selectProduct("HamSandwich");
            for(int i = 0; i < queryResult.size();i++) {
                if (queryResult.get(i) != null) {
                    double numPerOneProduct = app.selectQuantity("HamSandwich", queryResult.get(i));
                    switch (queryResult.get(i)) {
                        case "WhiteBread":
                            breadNeed = (numPerOneProduct * totalWeekly);
                            //weeklyResult.get(weeklyResult.size() - 1).WhiteBreadNeed;
                            if(breadNeed - dailyTransaction.get(0).WhiteBread_after > 0){
                                breadNeed = breadNeed - dailyTransaction.get(0).WhiteBread_after;
                                serviceSender.setContent("WhiteBread" + "-" + breadNeed);
                                serviceSender.setConversationId("Supplier");
                                myAgent.send(serviceSender);
                                //System.out.println(serviceSender);
                            }else {
                                breadNeed = 0;
                            }
                            break;
                        case "Ham":
                            hamNeed = (numPerOneProduct * totalWeekly);
                            if(hamNeed - dailyTransaction.get(0).Ham_after > 0){
                                hamNeed = hamNeed - dailyTransaction.get(0).Ham_after;
                                serviceSender.setContent("Ham" + "-" + hamNeed);
                                serviceSender.setConversationId("Supplier");
                                myAgent.send(serviceSender);
                                //System.out.println(serviceSender);

                            }else {
                                hamNeed = 0;
                            }
                            break;
                        case "Spread":
                            spreadNeed = (numPerOneProduct * totalWeekly);
                            if(spreadNeed - dailyTransaction.get(0).Spread_after > 0){
                                spreadNeed = spreadNeed - dailyTransaction.get(0).Spread_after;
                                serviceSender.setContent("Spread" + "-" + spreadNeed);
                                serviceSender.setConversationId("Supplier");
                                myAgent.send(serviceSender);
                                //System.out.println(serviceSender);
                            }else {
                                spreadNeed = 0;
                            }
                            break;
                    }
                }
            }
             */
        }
    }

    //Add a OneShot behavior to update the supplier stock and sending the data to  speciailist.
    private class updateStockFromSupplier extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null && msg.getConversationId().equals("Supplier")) {
                System.out.println(" \n Receive supply from supplier:  " + msg);
                String tempContent = msg.getContent();
                String[] arrOfStr = tempContent.split("-");
                String tempName = arrOfStr[0];
                String tempGrade = arrOfStr[1];
                double tempNumOf = Double.parseDouble(arrOfStr[2]);
                String tempAgentName = msg.getSender().getLocalName();

                //getting and extract the LocalDate
                LocalDate addedStockDate = LocalDate.parse(String.format("%s-%s-%s",arrOfStr[3],arrOfStr[4],arrOfStr[5]));

                supplierDataList.add(calcMethod.new supplierInfo(tempAgentName,tempName,tempGrade,tempNumOf,addedStockDate));	//update ingredient stock to list.
            }else {
                block();
            }
        }
        //Update list with update and compares current data.
    }
    //Receiving the request order from customers.

    private class receiveOrderFromCustomer extends CyclicBehaviour{
            public void action(){
                supplierDataList.trimToSize();
                customerDataList.trimToSize();

                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = myAgent.receive(mt);
                if(msg != null && msg.getConversationId().equals("customer")){
                    //System.out.println(">>>>>>>>>>>>>> Specialist Message from customer <<<<<<<<<<<<<<<<");
                    //System.out.println(msg);
                    //System.out.println("");
                    String tempContent = msg.getContent();
                    //myGui.displayUI(msg.getSender().toString() +"    " + msg.getConversationId());
                    String[] arrOfStr = tempContent.split("-");
                    String tempSenderName = msg.getSender().getLocalName();
                    String tempName = arrOfStr[0];
                    String tempGrade = arrOfStr[1];
                    int tempNumRequested = Integer.parseInt(arrOfStr[2]);
                    double pricePerUnit = Double.parseDouble(arrOfStr[3]);
                    double productCost = app.selectProductCost(tempName, tempGrade);
                    double utilityVal = utilityValueCalculation(1,tempNumRequested, pricePerUnit, productCost);
                    //adding the order request to customerList
                    customerDataList.add(calcMethod. new customerInfo(tempSenderName,tempName,tempGrade,tempNumRequested, pricePerUnit,0,1,utilityVal));
                    //System.out.println(customerDataList.get(customerDataList.size() -1).toStringOutput());
                    //Adding the request list in global.
                    //productChecklists.add(calcMethod.new productChecklist(tempName, tempGrade, tempNumRequested, 0));

                } else {
                    block();
                }
            }
    }

    //Specialist mechanism to optimize the avalable order and customer demand to maximize
    private class optimizeOrderFromcurrentStock extends OneShotBehaviour{
        AID[] customerList;
        HashMap<String,Double> dailyUpdate = new HashMap<String,Double>();
        public void action(){
            //memory allocation on arrayList
            dailyTransaction.clear();

            //latest weekly result index to wrire data.
            int latestIndex = weeklyResult.size() - 1;
            
            //summary data stock to contain in database.
            //int totalOrderReq = 0;
            //int totalOrderReject = 0;
            //int totalPaticipant = 0;
            double valueEarning = 0.0;
            int listSize = customerDataList.size();

            //Searching specialist agent and created address table.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdSearch = new ServiceDescription();
            sdSearch.setType("customer");
            template.addServices(sdSearch);
            try {
                DFAgentDescription[] searchResult = DFService.search(myAgent, template);
                customerList = new AID[searchResult.length];
                for (int i = 0; i < searchResult.length; ++i) {
                    customerList[i] = searchResult[i].getName();
                    //myGui.displayUI(customerList[i].getName());
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            //Checking the list of ingredient.
            for(int i = 0; i < supplierDataList.size(); i++){
                if(supplierDataList.get(i).numOfstock <= 0){
                    supplierDataList.remove(i);
                }
            }

            dailyTransaction.add(new ingredientTransaction(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0));

            //ingredient transaction (before matching).
            for(int i =0; i < supplierDataList.size();i++){
                if(dailyUpdate.get(supplierDataList.get(i).productName) == null){
                    dailyUpdate.put(supplierDataList.get(i).productName,supplierDataList.get(i).numOfstock);
                }else {
                    double tempInMap = dailyUpdate.get(supplierDataList.get(i).productName);
                    double newStock = tempInMap + supplierDataList.get(i).numOfstock;
                    dailyUpdate.replace(supplierDataList.get(i).productName,tempInMap,newStock);
                }
            }
            for(Map.Entry<String,Double> pair : dailyUpdate.entrySet()){
                stockDailyProduct("before",pair.getKey(),pair.getValue(),dailyTransaction);
            }
            dailyUpdate.clear();

            //Matching mechanism which call the methods from calC class.
            int numLoop = customerDataList.size();

            String matchingMethod = "";
            while(numLoop > 0){
                numLoop--;
                String productName = customerDataList.get(numLoop).orderName;
                String productGrade = customerDataList.get(numLoop).ingredientGrade;
                int numOfOrder = customerDataList.get(numLoop).numOfOrder;
                weeklyResult.get(latestIndex).numOfOrder = weeklyResult.get(latestIndex).numOfOrder + numOfOrder;

                //int numMatchingMethod = calcMethod.getRandIntRange(1,2);                      //ordering the optimization method.                                                                              
                int numMatchingMethod = 4;
                
                int productStockAvalable;

                System.out.println("\n Before");
                for(int i = 0; i < supplierDataList.size();i++){
                    System.out.println("       " + supplierDataList.get(i).toStringOutput());
                }

                //adding matching method name and policy
                if(numMatchingMethod == 1){
                    productStockAvalable = calcMethod.matchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                    matchingMethod = "traditional";
                }else if(numMatchingMethod == 2){
                    productStockAvalable = calcMethod.advMatchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                    matchingMethod = "advMatching";
                }else if(numMatchingMethod == 3){
                    productStockAvalable = calcMethod.advWithExpireDate(supplierDataList, productName, productGrade, numOfOrder, dayTimeCount);
                    matchingMethod = "advWithExpireDate";
                }else {
                    productStockAvalable = calcMethod.newMarketMatching(supplierDataList, productName, productGrade, numOfOrder, dayTimeCount);
                    matchingMethod = "newMarketMatching";
                }

                System.out.println(String.format("\n Method:  %s  Day %d  num of accept %d   agent name: %s",matchingMethod, dayTimeCount, productStockAvalable, customerDataList.get(numLoop).agentName));

                System.out.println("\n After");
                for(int i = 0; i < supplierDataList.size();i++){
                    System.out.println("       " + supplierDataList.get(i).toStringOutput());
                }

                weeklyResult.get(latestIndex).numOfAccept = weeklyResult.get(latestIndex).numOfAccept + productStockAvalable;
                weeklyResult.get(latestIndex).numOfReject = weeklyResult.get(latestIndex).numOfReject + (numOfOrder - productStockAvalable);

                dailyTransaction.get(dailyTransaction.size() - 1).numOfOrder = dailyTransaction.get(dailyTransaction.size() - 1).numOfOrder + numOfOrder;
                dailyTransaction.get(dailyTransaction.size() - 1).numOfAccept = dailyTransaction.get(dailyTransaction.size() - 1).numOfAccept + productStockAvalable;
                dailyTransaction.get(dailyTransaction.size() - 1).numOfReject = dailyTransaction.get(dailyTransaction.size() - 1).numOfReject + (numOfOrder - productStockAvalable);

                //Matching market (value only method).
                //int productStockAvalable = calcMethod.matchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                //int productStockAvalable = calcMethod.advMatchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                //int productStockAvalable = calcMethod.advWithExpireDate(supplierDataList, productName, productGrade, numOfOrder);

                        //myAgent.doSuspend();

                //Matching market (value and prioritized ingredient expired date require).

                //myGui.displayUI(String.format(" The number of available order from stock is:  %s\n", productStockAvalable));
                customerDataList.get(numLoop).numReply = productStockAvalable;
                //valueEarning = valueEarning + (customerDataList.get(numLoop).numReply * customerDataList.get(numLoop).pricePerUnit);            //Total earned value for each customer.
                //totalOrderReq = totalOrderReq + numOfOrder;
                //totalOrderReject = totalOrderReject + (numOfOrder - customerDataList.get(numLoop).numReply);

                //reply order to customer (ACCEPT_PROPOSAL to winner and REJECT_PROPOSAL to others).
                if(productStockAvalable > 0){
                    //proposed some product to them.
                    ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    acceptMsg.setContent(customerDataList.get(numLoop).toUpdateService());
                    acceptMsg.setConversationId("reply-to-customer");
                    for (int i = 0; i < customerList.length; i++){
                        if(customerList[i].getLocalName().equals(customerDataList.get(numLoop).agentName)){
                            acceptMsg.addReceiver(customerList[i]);
                        }
                    }
                    myAgent.send(acceptMsg);
                    //myGui.displayUI(acceptMsg.toString());
                    //System.out.println(acceptMsg);
                }else {
                    //proposed some product to them.
                    ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    //acceptMsg.setContent(customerDataList.get(numLoop).toUpdateService());
                    rejectMsg.setConversationId("reply-to-customer");
                    for (int i = 0; i < customerList.length; i++){
                        if(customerList[i].getLocalName().equals(customerDataList.get(numLoop).agentName)){
                            rejectMsg.addReceiver(customerList[i]);
                        }
                    }
                    myAgent.send(rejectMsg);
                    //System.out.println(rejectMsg);
                    //myGui.displayUI(rejectMsg.toString());
                }
            }
            
            //updating customer data list.
            //totalPaticipant = customerDataList.size();              //total participant count.
            while(listSize > 0){
                customerDataList.remove(0);
                listSize--;
            }

            //ingredient transaction (after matching).
            for(int i =0; i < supplierDataList.size();i++){
                if(dailyUpdate.get(supplierDataList.get(i).productName) == null){
                    dailyUpdate.put(supplierDataList.get(i).productName,supplierDataList.get(i).numOfstock);
                }else {
                    double tempInMap = dailyUpdate.get(supplierDataList.get(i).productName);
                    double newStock = tempInMap + supplierDataList.get(i).numOfstock;
                    dailyUpdate.replace(supplierDataList.get(i).productName,tempInMap,newStock);
                }
            }
            for(Map.Entry<String,Double> pair : dailyUpdate.entrySet()){
                stockDailyProduct("after",pair.getKey(),pair.getValue(),dailyTransaction);
            }
            dailyUpdate.clear();

            // adding totalPaticipant, winner, lost
            //dailyTransaction.get(0).totalPaticipant = totalPaticipant;

            try {
                calcMethod.updateCSVFile(dailyResult,dailyTransaction.get(0).rowData());
                myGui.displayUI("Daily transaction:  " + dailyTransaction.get(0).stringDisplay() + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //Order prediction method that is applied for all agent types.
    private double standardOptimzation (int percentage,String ingradName, ArrayList<weeklyResult> weeklyResult){
        double result = 0;
        System.out.println("Ingredient request method : Standard method");      //The standard method that refilled ingredient stock based on maximum order for current week.
        int totalWeekly = 0;
        if(weeklyResult.size() > 1){
            totalWeekly = weeklyResult.get(weeklyResult.size() - 2).numOfOrder;
        }else{
            totalWeekly = weeklyResult.get(weeklyResult.size() - 1).numOfOrder;
        }

        System.out.println("                        total order get :      " + weeklyResult.size() + "       " + totalWeekly);
        //int totalWeekly = weeklyResult.get(weeklyResult.size() - 1).numOfOrder;
        int totalReq = totalWeekly + ((totalWeekly * percentage)/100);
        switch (ingradName){
            case "WhiteBread":
                double breadNeed = ((app.selectQuantity("HamSandwich", "WhiteBread")) * totalReq);
                result = breadNeed;
                break;
            case "Ham":
                double hamNeed = ((app.selectQuantity("HamSandwich", "Ham")) * totalReq);
                result = hamNeed;
                break;
            case "Spread":
                double spreadNeed = ((app.selectQuantity("HamSandwich", "Spread")) * totalReq);
                result = spreadNeed;
                break;
        }
        return result;
    }

    private double smaOptimaization (int windowSize, int percentage, String ingradName, ArrayList<weeklyResult> weeklyResult){
        double result = 0;
        System.out.println("Ingredient request method : Sliding Moving Average Optimization");      //The standard method that refilled ingredient stock based on maximum order for current week.
        int histIdx = weeklyResult.size();
        int avgOrder = 0;
        if(histIdx < windowSize){
            //the average of total order (previous to current week) is applied for next week calculation if number of window for SMA is not enought.
            for (int i = 0; i < weeklyResult.size();i++){
                avgOrder = avgOrder + weeklyResult.get(i).numOfOrder;
            }
            avgOrder = avgOrder/histIdx;
        }else {
            //SMA Calculation based on num of window required.
            for(int i = histIdx - windowSize; i < histIdx; i++){
                avgOrder = avgOrder + weeklyResult.get(i).numOfOrder;
            }
            avgOrder = avgOrder/windowSize;
        }

        int totalReq = avgOrder + (avgOrder * (percentage/100));

        switch (ingradName){
            case "WhiteBread":
                double breadNeed = ((app.selectQuantity("HamSandwich", "WhiteBread")) * totalReq);
                result = breadNeed;
                break;
            case "Ham":
                double hamNeed = ((app.selectQuantity("HamSandwich", "Ham")) * totalReq);
                result = hamNeed;
                break;
            case "Spread":
                double spreadNeed = ((app.selectQuantity("HamSandwich", "Spread")) * totalReq);
                result = spreadNeed;
                break;
        }
        return result;
    }

    private class ingredientTransaction{
        public int totalPaticipant, numOfOrder,numOfAccept, numOfReject;
        public double WhiteBread, WhiteBread_after, Ham, Ham_after, Onion, Onion_after,Pickle, Pickle_after, Tuna, Tuna_after, Spread, Spread_after;
        public ingredientTransaction(int totalPaticipant, int numOfOrder, int numOfAccept,int numOfReject,int WhiteBread, double WhiteBread_after, double Ham, double Ham_after, double Onion, double Onion_after,
                                     double Pickle, double Pickle_after, double Tuna, double Tuna_after, double Spread, double Spread_after){
            this.totalPaticipant = totalPaticipant;
            this.numOfOrder = numOfOrder;
            this.numOfAccept = numOfAccept;
            this.numOfReject = numOfReject;
            this.WhiteBread = WhiteBread;
            this.WhiteBread_after = WhiteBread_after;
            this.Ham = Ham;
            this.Ham_after = Ham_after;
            this.Onion = Onion;
            this.Onion_after = Onion_after;
            this.Pickle = Pickle;
            this.Pickle_after = Pickle_after;
            this.Tuna = Tuna;
            this.Tuna_after = Tuna_after;
            this.Spread = Spread;
            this.Spread_after = Spread_after;
        }
        public String[] indexCSV(){
            String[] resultIndex = {"totalPaticipant", "totalOrder", "totalOrderAccept","totalOrderReject", "WB", "WB_after", "Ham", "Ham_after", "Onion", "Onion_after", "Pickle", "Pickle_after", "Tuna", "Tuna_after", "Spread", "Spread_after"};
            return resultIndex;
        }
        public String[] rowData(){
            String[] result = {String.valueOf(this.totalPaticipant),String.valueOf(this.numOfOrder),String.valueOf(this.numOfAccept),String.valueOf(this.numOfReject),
                    String.valueOf(this.WhiteBread),String.valueOf(this.WhiteBread_after),String.valueOf(this.Ham),String.valueOf(this.Ham_after),String.valueOf(this.Onion),
                    String.valueOf(Onion_after),String.valueOf(Pickle),String.valueOf(this.Pickle_after),String.valueOf(this.Tuna),String.valueOf(this.Tuna_after),
                    String.valueOf(this.Spread),String.valueOf(this.Spread_after)};
            return result;
        }
        public String stringDisplay(){
            String stringOutput = String.format("Accepted order: %d  Rejected order: %d ",this.totalPaticipant,this.numOfAccept,this.numOfReject);
            return stringOutput;
        }

    }

    private class weeklyResult{
        public String orderName;
        public int numOfOrder, numOfAccept, numOfReject;
        public double WhiteBread, WhiteBreadNeed, Ham, HamNeed, Onion, OnionNeed,Pickle, PickleNeed, Tuna, TunaNeed, Spread, SpreadNeed;
        public weeklyResult(String orderName, int numOfOrder, int numOfAccept, int numOfReject, double WhiteBread, double WhiteBreadNeed, double Ham, double HamNeed, double Onion, double OnionNeed,
                            double Pickle, double PickleNeed, double Tuna, double TunaNeed, double Spread, double SpreadNeed  ){
            this.orderName = orderName;
            this.numOfOrder = numOfOrder;
            this.numOfAccept = numOfAccept;
            this.numOfReject = numOfReject;
            this.WhiteBread = WhiteBread;
            this.WhiteBreadNeed = WhiteBreadNeed;
            this.Ham = Ham;
            this.HamNeed = HamNeed;
            this.Onion = Onion;
            this.OnionNeed = OnionNeed;
            this.Pickle = Pickle;
            this.PickleNeed = PickleNeed;
            this.Tuna = Tuna;
            this.TunaNeed = TunaNeed;
            this.Spread = Spread;
            this.SpreadNeed = SpreadNeed;
        }
        public String[] indexCSV(){
            String[] resultIndex = {"orderName","numOfOrder","numOfaccept","numOfReject","WhiteBread","WhiteBreadNeed","Ham","HamNeed","OnionNeed",
                    "Onion","Pickle","PickleNeed","Tuna","TunaNeed","Spread","SpreadNeed"};
            return resultIndex;
        }
        public String[] rowData(){
            String[] result = {String.valueOf(this.orderName),String.valueOf(this.numOfOrder),String.valueOf(this.numOfAccept),String.valueOf(this.numOfReject),
                    String.valueOf(this.WhiteBread),String.valueOf(this.WhiteBreadNeed),String.valueOf(this.Ham),String.valueOf(this.HamNeed),String.valueOf(this.Onion),
                    String.valueOf(OnionNeed),String.valueOf(Pickle),String.valueOf(this.PickleNeed),String.valueOf(this.Tuna),String.valueOf(this.TunaNeed),
                    String.valueOf(this.Spread),String.valueOf(this.SpreadNeed)};
            return result;
        }
        public String toUpdateService(String ingradName){
            String message = String.format("%s-");
            return  message;
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

    private void stockDailyProduct(String period, String ingradName, double quantity, ArrayList<ingredientTransaction> ingredientTransaction){
        switch (period){
            case "before":
                if(ingradName.equals("WhiteBread")){
                    ingredientTransaction.get(0).WhiteBread = quantity;
                }else if(ingradName.equals("Ham")){
                    ingredientTransaction.get(0).Ham = quantity;
                }else if(ingradName.equals("Onion")){
                    ingredientTransaction.get(0).Onion = quantity;
                }else if(ingradName.equals("Pickle")){
                    ingredientTransaction.get(0).Pickle = quantity;
                }else if(ingradName.equals("Tuna")){
                    ingredientTransaction.get(0).Tuna = quantity;
                }else if(ingradName.equals("Spread")){
                    ingredientTransaction.get(0).Spread = quantity;
                }
                break;
            case "after":
                if(ingradName.equals("WhiteBread")){
                    ingredientTransaction.get(0).WhiteBread_after = quantity;
                }else if(ingradName.equals("Ham")){
                    ingredientTransaction.get(0).Ham_after = quantity;
                }else if(ingradName.equals("Onion")){
                    ingredientTransaction.get(0).Onion_after = quantity;
                }else if(ingradName.equals("Pickle")){
                    ingredientTransaction.get(0).Pickle_after = quantity;
                }else if(ingradName.equals("Tuna")){
                    ingredientTransaction.get(0).Tuna_after = quantity;
                }else if(ingradName.equals("Spread")){
                    ingredientTransaction.get(0).Spread_after = quantity;
                }
                break;
        }
    }

    private void sellingTransactionDaily(String period, String orderName, int qunatity){

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
}


/*
            previous database saving module.
            app.insertResult("MatchingResult", matchingMethod, totalPaticipant,winner,lost,totalOrderReq, totalOrderReject, valueEarning);
            app.insertSupplier(writtingIngrad.get(0),writtingIngrad.get(1),writtingIngrad.get(2),writtingIngrad.get(3),writtingIngrad.get(4),writtingIngrad.get(5),writtingIngrad.get(6),
                writtingIngrad.get(7),writtingIngrad.get(8),writtingIngrad.get(9),writtingIngrad.get(10),writtingIngrad.get(11),writtingIngrad.get(12),writtingIngrad.get(13),writtingIngrad.get(14),
                writtingIngrad.get(15),writtingIngrad.get(16),writtingIngrad.get(17),writtingIngrad.get(18),writtingIngrad.get(19),writtingIngrad.get(20));
            app.insertAllResult(ingredientTransaction.get(0).WhiteBreadA,ingredientTransaction.get(0).WhiteBreadA_after,ingredientTransaction.get(0).WhiteBreadB,ingredientTransaction.get(0).WhiteBreadB_after,ingredientTransaction.get(0).WhiteBreadC,ingredientTransaction.get(0).WhiteBreadC_after,
                ingredientTransaction.get(0).HamA,ingredientTransaction.get(0).HamA_after,ingredientTransaction.get(0).HamB,ingredientTransaction.get(0).HamB_after,ingredientTransaction.get(0).HamC,ingredientTransaction.get(0).HamC_after,
                ingredientTransaction.get(0).MatureCheddarMilkA,ingredientTransaction.get(0).MatureCheddarMilkA_after,ingredientTransaction.get(0).MatureCheddarMilkB,ingredientTransaction.get(0).MatureCheddarMilkB_after,ingredientTransaction.get(0).MatureCheddarMilkC,ingredientTransaction.get(0).MatureCheddarMilkC_after,
                ingredientTransaction.get(0).OnionA,ingredientTransaction.get(0).OnionA_after,ingredientTransaction.get(0).OnionB,ingredientTransaction.get(0).OnionB_after,ingredientTransaction.get(0).OnionC,ingredientTransaction.get(0).OnionC_after,
                ingredientTransaction.get(0).PickleA,ingredientTransaction.get(0).PickleA_after,ingredientTransaction.get(0).PickleB,ingredientTransaction.get(0).PickleB_after,ingredientTransaction.get(0).PickleC,ingredientTransaction.get(0).PickleC_after,
                ingredientTransaction.get(0).TunaA,ingredientTransaction.get(0).TunaA_after,ingredientTransaction.get(0).TunaB,ingredientTransaction.get(0).TunaB_after,ingredientTransaction.get(0).TunaC,ingredientTransaction.get(0).TunaC_after,
                ingredientTransaction.get(0).SunflowerSpreadA,ingredientTransaction.get(0).SunflowerSpreadA_after,ingredientTransaction.get(0).SunflowerSpreadB,ingredientTransaction.get(0).SunflowerSpreadB_after,ingredientTransaction.get(0).SunflowerSpreadC,ingredientTransaction.get(0).SunflowerSpreadC_after);
            app.orderTransaction("OrderTransaction", orderTransaction.get(0).HamCheeseSandwichA_order, orderTransaction.get(0).HamCheeseSandwichA_accept, orderTransaction.get(0).HamCheeseSandwichB_order, orderTransaction.get(0).HamCheeseSandwichB_accept, orderTransaction.get(0).HamCheeseSandwichC_order, orderTransaction.get(0).HamCheeseSandwichC_accept,
                orderTransaction.get(0).HamSandwichA_order, orderTransaction.get(0).HamSandwichA_accept, orderTransaction.get(0).HamSandwichB_order, orderTransaction.get(0).HamSandwichB_accept, orderTransaction.get(0).HamSandwichC_order, orderTransaction.get(0).HamSandwichC_accept,
                orderTransaction.get(0).CheeseOnionA_order, orderTransaction.get(0).CheeseOnionA_accept, orderTransaction.get(0).CheeseOnionB_order, orderTransaction.get(0).CheeseOnionB_accept,orderTransaction.get(0).CheeseOnionC_order, orderTransaction.get(0).CheeseOnionC_accept,
                orderTransaction.get(0).CheesePickleA_order, orderTransaction.get(0).CheesePickleA_accept, orderTransaction.get(0).CheesePickleB_order, orderTransaction.get(0).CheesePickleB_accept, orderTransaction.get(0).CheesePickleC_order, orderTransaction.get(0).CheesePickleC_accept,
                orderTransaction.get(0).TunaA_order, orderTransaction.get(0).TunaA_accept, orderTransaction.get(0).TunaB_order, orderTransaction.get(0).TunaB_accept, orderTransaction.get(0).TunaC_order, orderTransaction.get(0).TunaC_accept);
            */