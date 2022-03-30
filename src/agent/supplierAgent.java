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

    DecimalFormat df = new DecimalFormat("#.##");
    DatabaseConn app = new DatabaseConn();
    calcMethod calc = new calcMethod();

    // The catalogue of supply items (maps the title name to its quantities)
    ArrayList<supplierInfo> sellingProductList = new ArrayList<>();
    ArrayList<weeklyReport> lastWeekDeliver = new ArrayList<>();

    int orderTimer = 10000;
    int dayCount = 0;
    int weekCount = 0;

    double numOfStock = 100000;
    String supplyPath = "large-10k-Spike15D-supplyResult";

    //int[] orderTimerArray = {40000,70000};

    // The GUI by means of which the user can add books in the catalogue
    //public supplierUI myGui;

    String fileClasspath = String.format("C:\\Users\\Krist\\VSCode\\DigiSandwich_Release_2\\output\\%s.csv",supplyPath);      //Home PC classpath
    //String fileClasspath = String.format("C:\\Users\\KChiewchanadmin\\IdeaProjects\\DigiSandwich_Release_2\\output\\%s.csv",supplyPath);      //NB office classpath

    protected void setup() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            String[] entryWeekly = {"Week","WhiteBread","Ham","Onion","Pickle","Tuna","Spread"};
            calc.createCSV(fileClasspath,entryWeekly);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create and show the GUI
            //myGui = new supplierUI(this);
            //myGui.showGui();

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
        addBehaviour(new nextWeekRequest());

        //First week intialise for Raynor's stock
        sellingProductList.add(new supplierInfo(getLocalName(),"WhiteBread","general",numOfStock,1));
        sellingProductList.add(new supplierInfo(getLocalName(),"Ham","general",numOfStock,1));
        sellingProductList.add(new supplierInfo(getLocalName(),"Spread","general",numOfStock,1));

        //Add a TickerBehaviour to refill supply (1 time a week).
        addBehaviour(new TickerBehaviour(this, orderTimer){
            protected void onTick() {
                //Day count added.
                dayCount++;
                String day = calc.dayInWeek(dayCount);
                //Checking the weekly list order.
                if(day == "Monday"){
                    weekCount++;
                    if(lastWeekDeliver.size() <= 1){
                        lastWeekDeliver.add(new weeklyReport(weekCount,0,0,0,0,0,0));
                        for (int i=0; i < sellingProductList.size();i++){
                            //System.out.println(sellingProductList.get(i).toUpdateService());
                            updateProduct(getLocalName(),sellingProductList.get(i).productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
                            String productName = sellingProductList.get(i).productName;
                            switch (productName){
                                case "WhiteBread":
                                    lastWeekDeliver.get(0).WhiteBread = sellingProductList.get(i).numOfstock;
                                    break;
                                case "Ham":
                                    lastWeekDeliver.get(0).Ham = sellingProductList.get(i).numOfstock;
                                    break;
                                case "Spread":
                                    lastWeekDeliver.get(0).Spread = sellingProductList.get(i).numOfstock;
                                    break;
                            }
                            sellingProductList.get(i).status = 0;
                        }
                    }else{
                        //getting the data from the week - 1 (size - 2)
                        int currentIdx = lastWeekDeliver.size() - 2;
                        for (int i = 0; i < sellingProductList.size();i++){
                            if(sellingProductList.get(i).status == 1){
                                String ingradName = sellingProductList.get(i).productName;
                                switch (ingradName){
                                    case "WhiteBread":
                                        updateProduct(getLocalName(),"WhiteBread","general",lastWeekDeliver.get(currentIdx).WhiteBread);
                                        sellingProductList.get(i).status = 0;
                                        break;
                                    case "Ham":
                                        updateProduct(getLocalName(),"Ham","general",lastWeekDeliver.get(currentIdx).Ham);
                                        sellingProductList.get(i).status = 0;
                                        break;
                                    case "Spread":
                                        updateProduct(getLocalName(),"Spread","general",lastWeekDeliver.get(currentIdx).Spread);
                                        sellingProductList.get(i).status = 0;
                                        break;
                                }
                            }
                        }
                    }
                }

                if(day == "Saturday"){
                    if(lastWeekDeliver.size() == 1){
                        try {
                            calc.updateCSVFile(fileClasspath,lastWeekDeliver.get(0).rowData());
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    //Writing request ingredient to lastweekOrder list.

                    System.out.println(" num of row:   " + lastWeekDeliver.size());
                    lastWeekDeliver.add(new weeklyReport(weekCount,0,0,0,0,0,0));
                    int lastIndex = lastWeekDeliver.size() - 1;
                    for(int i = 0; i < sellingProductList.size();i++){
                        if(sellingProductList.get(i).status == 1){
                            String productName = sellingProductList.get(i).productName;
                            switch (productName){
                                case "WhiteBread":
                                    lastWeekDeliver.get(lastIndex).WhiteBread = sellingProductList.get(i).numOfstock;
                                    break;
                                case "Ham":
                                    lastWeekDeliver.get(lastIndex).Ham = sellingProductList.get(i).numOfstock;
                                    break;
                                case "Spread":
                                    lastWeekDeliver.get(lastIndex).Spread = sellingProductList.get(i).numOfstock;
                                    break;
                            }
                            //sellingProductList.get(i).status = 0;
                        }
                    }
                    System.out.println(" last week delivery:   " + lastWeekDeliver.get(lastIndex).toString());
                    if(lastWeekDeliver.size() > 1) {
                        System.out.println("latest week update to csv:   " + lastWeekDeliver.size());
                        try {
                            calc.updateCSVFile(fileClasspath, lastWeekDeliver.get(lastWeekDeliver.size() - 1).rowData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //addBehaviour(new replyToSpecialist());
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

    private class nextWeekRequest extends CyclicBehaviour{
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
                        sellingProductList.get(i).status = 1;
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
        public int status;

        public supplierInfo(String agentName, String productName, String ingredientGrade, double numOfstock, int status) {
            this.agentName = agentName;
            this.productName = productName;
            this.ingredientGrade = ingredientGrade;
            this.numOfstock = numOfstock;
            this.status = status;
        }

        public String toStringOutput() {
            return "Agent name: "+ this.agentName + " Ingredient name: " + this.productName + "   Quality: " + this.ingredientGrade + "   Stock: " + df.format(numOfstock) + " Re-stock status: " + this.status;
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
    }
}

