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

import java.time.LocalDate;
import java.util.ArrayList;


public class specialistAgent extends Agent {
    //private biddingSpecialistUI myGui;

    //Initial Data table and others parameters.

    ArrayList<calcMethod.supplierInfo> supplierDataList = new ArrayList<>();   //List of available ingredient
    ArrayList<calcMethod.customerInfo> customerDataList = new ArrayList<>();   //List of request orders.
    //ArrayList<calcMethod.productChecklist> productChecklists = new ArrayList<>();
    ArrayList<calcMethod.ingredientTable> ingredientWritting = new ArrayList<>();

    ArrayList<calcMethod.orderTransaction> orderTransaction = new ArrayList<>();
    ArrayList<calcMethod.ingredientTransaction> ingredientTransaction = new ArrayList<>();
    ArrayList<Double> writtingIngrad = new ArrayList<>();

    int dayTimeCount = 0;

    DatabaseConn app = new DatabaseConn();
    calcMethod calcMethod = new calcMethod();

    //Updating agent services
    protected void setup() {
        //Gui active.
        // Create and show the GUI
        //myGui = new biddingSpecialistUI(this);
        //myGui.show();

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

        addBehaviour(new specialistAgent.updateIngredientStock());
        addBehaviour(new specialistAgent.containServiceOrder());
        
        //The service reply process which is after the end of auction. Agent 

        //Add a TickerBehaviour that shows the list of ingredients stock and order queue.
        addBehaviour(new TickerBehaviour(this, 60000){
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
                if(dayTimeCount < 31){
                    dayTimeCount++;
                    addBehaviour(new optimizeOrderFromcurrentStock());
                    addBehaviour(new specialistAgent.stopDeliveryIngrad());
                }else{
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

    //Stop delivery ingredient if it has huge stock in manufacture.
    private class stopDeliveryIngrad extends OneShotBehaviour{
        public  void action(){
            //Checking the current stock for all ingredients.

            //Sending the PROPOSE message to supplier agents about the delivery stock schedule (refill stock / do not refill).
            //Clear temporary data before next day matching mechanism process.
        }
    }


    //Add a OneShot behavior to update the supplier stock and sending the data to  speciailist.
    private class updateIngredientStock extends CyclicBehaviour {
        public void action() {
            supplierDataList.trimToSize();
            customerDataList.trimToSize();
            
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null && msg.getConversationId().equals("Supplier")) {
                String tempContent = msg.getContent();
                String[] arrOfStr = tempContent.split("-");
                String tempName = arrOfStr[0];
                String tempGrade = arrOfStr[1];
                double tempNumOf = Double.parseDouble(arrOfStr[2]);
                String tempAgentName = msg.getSender().getLocalName();

                //Writing supplier data.
                ingredientWritting.add(calcMethod.new ingredientTable(tempName, tempGrade,tempNumOf,0,0));

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

    private class containServiceOrder extends CyclicBehaviour{
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
                    double utilityVal = calcMethod.utilityValueCalculation(1,tempNumRequested, pricePerUnit, productCost);

                    //adding the order request to customerList
                    customerDataList.add(calcMethod.new customerInfo(tempSenderName,tempName,tempGrade,tempNumRequested, pricePerUnit,0,1,utilityVal));

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
        public void action(){
            //memory allocation on arrayList
            orderTransaction.clear();
            ingredientTransaction.clear();
            
            //summary data stock to contain in database.
            int winner = 0;
            int lost = 0;
            int totalOrderReq = 0;
            int totalOrderReject = 0;
            int totalPaticipant = 0;
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

            //list of order for this round.
            /**
            if(customerDataList.size() > 0){
                System.out.println("");
                for (agent.calcMethod.customerInfo a : customerDataList) {
                    System.out.println("Before bidding process:   " + a.toStringOutput());
                    
                }
                System.out.println("");
            }else{
                System.out.println("Do not have order request today");
            }
             */ 
            
            //Initialize the order transaction.
            //ArrayList<calcMethod.orderTransaction> orderTransaction = new ArrayList<>();
            orderTransaction.add(calcMethod.new orderTransaction(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0));
            //ArrayList<calcMethod.ingredientTransaction> ingredientTransaction = new ArrayList<>();
            ingredientTransaction.add(calcMethod.new ingredientTransaction(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
            


            //ingredient transaction loop (before).
            for (int i = 0; i < supplierDataList.size(); i++){
                calcMethod.ingradientTransactionBefore(supplierDataList.get(i).productName, supplierDataList.get(i).ingredientGrade, supplierDataList.get(i).numOfstock, ingredientTransaction);
            }
            
            //Matching mechanism which call the methods from calC class.
            int numLoop = customerDataList.size();

            String matchingMethod = "";
            while(numLoop > 0){
                numLoop--;
                String productName = customerDataList.get(numLoop).orderName;
                String productGrade = customerDataList.get(numLoop).ingredientGrade;
                int numOfOrder = customerDataList.get(numLoop).numOfOrder;

                calcMethod.orderTransactionReq(productName, productGrade, numOfOrder, orderTransaction);

                //int numMatchingMethod = calcMethod.getRandIntRange(1,2);                      //ordering the optimization method.                                                                              
                int numMatchingMethod = 3;
                
                int productStockAvalable;

                //adding matching method name and policy
                if(numMatchingMethod == 1){
                    productStockAvalable = calcMethod.matchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                    matchingMethod = "traditional";
                }else if(numMatchingMethod == 2){
                    productStockAvalable = calcMethod.advMatchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                    matchingMethod = "advMatching";
                }else {
                    productStockAvalable = calcMethod.advWithExpireDate(supplierDataList, productName, productGrade, numOfOrder, dayTimeCount);
                    matchingMethod = "advWithExpireDate";
                }

                calcMethod.orderTransactionAccept(productName, productGrade, productStockAvalable, orderTransaction);

                //Matching market (value only method).
                //int productStockAvalable = calcMethod.matchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                //int productStockAvalable = calcMethod.advMatchingOrder(supplierDataList, productName, productGrade, numOfOrder);
                //int productStockAvalable = calcMethod.advWithExpireDate(supplierDataList, productName, productGrade, numOfOrder);

                        //myAgent.doSuspend();

                //Matching market (value and prioritized ingredient expired date require).

                //myGui.displayUI(String.format(" The number of available order from stock is:  %s\n", productStockAvalable));
                customerDataList.get(numLoop).numReply = productStockAvalable;
                valueEarning = valueEarning + (customerDataList.get(numLoop).numReply * customerDataList.get(numLoop).pricePerUnit);            //Total earned value for each customer.
                totalOrderReq = totalOrderReq + numOfOrder;
                totalOrderReject = totalOrderReject + (numOfOrder - customerDataList.get(numLoop).numReply);

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
                    winner++;                                       //Number of winner to buy counting.
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
                    lost++;                                         //Number of participants who do not receive orders.
                }
            }
            
            //updating customer data list.
            totalPaticipant = customerDataList.size();              //total participant count.
            while(listSize > 0){
                customerDataList.remove(0);
                listSize--;
            }

            //ingredient transaction loop (after).
            for (int i = 0; i < supplierDataList.size(); i++){
                calcMethod.ingradientTransactionAfter(supplierDataList.get(i).productName, supplierDataList.get(i).ingredientGrade, supplierDataList.get(i).numOfstock, ingredientTransaction);
            } 

            //ArrayList<Double> allWritingResult = new ArrayList<>();
            //ArrayList<Double> writtingIngrad = new ArrayList<>();
            for(int i = 0; i < supplierDataList.size();i++){
                for(int j = 0; j < ingredientWritting.size();j++){
                    if(supplierDataList.get(i).productName.equals(ingredientWritting.get(j).ingredientName) &&
                            supplierDataList.get(i).ingredientGrade.equals(ingredientWritting.get(j).ingredientGrade)){
                        ingredientWritting.get(j).numOfAfter = supplierDataList.get(i).numOfstock;
                        ingredientWritting.get(j).percentage = (ingredientWritting.get(j).numOfAfter * 100)/ingredientWritting.get(i).numOfBefore;
                        writtingIngrad.add(ingredientWritting.get(j).percentage);
                        //allWritingResult.add(ingredientWritting.get(j).numOfBefore);
                        //allWritingResult.add(ingredientWritting.get(j).numOfAfter);
                    }
                }
            }
            /**
            for (agent.calcMethod.ingredientTable a : ingredientWritting
                 ) {
                System.out.println(a.toString());
            }
             */
            /***
            for(int i = 0; i < writtingIngrad.size();i++){
                System.out.println(writtingIngrad.get(i));
            ***/
            //output.append(matchingMethod);

            
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

            //writtingIngrad.clear();
            //orderTransaction.clear();
            //ingredientTransaction.clear();
        }
    }

    /***
    //Reply process to all customers, update stock and clear the market.
    private class responseToCustomers extends OneShotBehaviour{
    	private AID[] customerList;
        public void action(){
        	//sorted list to find the highest

        	//Searching specialist agent and created address table.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdSearch = new ServiceDescription();
            sdSearch.setType("customer");
            template.addServices(sdSearch);
            try {
                DFAgentDescription[] searchResult = DFService.search(myAgent, template);
                customerList = new AID[searchResult.length];
                for (int j = 0; j < searchResult.length; ++j) {
                	customerList[j] = searchResult[j].getName();
                }
                //System.out.println(specialistAgent[0].getName());
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }
     */
}

/**
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
 */