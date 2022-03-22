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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;

public class supplierAgent extends Agent {

    DecimalFormat df = new DecimalFormat("#.##");
    DatabaseConn app = new DatabaseConn();
    calcMethod calcMethod = new calcMethod();
    HashMap weeklyUpdate = new HashMap();

    // The catalogue of supply items (maps the title name to its quantities)
    ArrayList<supplierInfo> sellingProductList = new ArrayList<>();

    int orderTimer = 70000;
    double numOfStock = 150000;
    int dayCount = 0;

    //int[] orderTimerArray = {40000,70000};

    // The GUI by means of which the user can add books in the catalogue
    //public supplierUI myGui;

    protected void setup() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
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
        addBehaviour(new nextWeekDelivery());

        //First week intialise for Raynor's stock
        sellingProductList.add(new supplierInfo(getLocalName(),"WhiteBread","general",numOfStock,1));
        sellingProductList.add(new supplierInfo(getLocalName(),"Ham","general",numOfStock,1));
        sellingProductList.add(new supplierInfo(getLocalName(),"Spread","general",numOfStock,1));

        //auto update stock which follow the updateProduct method(OneShotBehaviour).

        for (int i=0; i < sellingProductList.size();i++){
            //System.out.println(sellingProductList.get(i).toUpdateService());
            updateProduct(getLocalName(),sellingProductList.get(i).productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
            sellingProductList.get(i).status = 0;
        }

        //Add a TickerBehaviour to refill supply (1 time a week).
        addBehaviour(new TickerBehaviour(this, orderTimer){
            protected void onTick() {
                //Day count added.
                dayCount++;

                //Clearing the weekly stock number.
                weeklyUpdate.clear();

                //Cheking and update the stock for delivery for next time delivery
                for (int i=0; i < sellingProductList.size();i++){
                    if(sellingProductList.get(i).status == 1){
                        updateProduct(getLocalName(),sellingProductList.get(i).productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
                        sellingProductList.get(i).status = 0;
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

                //System.out.println(" \n Service sender from supplier:     " + serviceSender);

            }
        } );
    }

    private class nextWeekDelivery extends CyclicBehaviour{

        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            //System.out.println("Request receiving:  " + msg);
            if(msg != null && msg.getConversationId().equals("Supplier")){
                //System.out.println(" \n Request receiving:  " + msg);
                String[] arrOfStr = msg.getContent().split("-");
                String tempName = arrOfStr[0];
                double tempNumRequested = Double.parseDouble(arrOfStr[1]);
                for(int i = 0; i < sellingProductList.size();i++){
                    if(tempName.equals(sellingProductList.get(i).productName)){
                        sellingProductList.get(i).numOfstock = tempNumRequested;
                        sellingProductList.get(i).status = 1;
                    }
                }
                //Sending the ACCEPT message to confirm next week order.
                ACLMessage replyMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                replyMsg.setConversationId("reply-to-Specialist");
                replyMsg.addReceiver(msg.getSender());
                System.out.print("Message from suppliers:  " + replyMsg);

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
        public double WhiteBread;
        public double Ham;
        public double Onion;
        public double Pickle;
        public double Tuna;
        public double Spread;

        public weeklyReport(double WhieBread, double Ham, double Onion, double Pickle, double Tuna, double Spread){
            this.WhiteBread = WhieBread;
            this.Ham = Ham;
            this.Onion = Onion;
            this.Pickle = Pickle;
            this.Tuna = Tuna;
            this.Spread = Spread;
        }
    }
}

