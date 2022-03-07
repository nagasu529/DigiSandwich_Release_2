package agent;

import ui.supplierUI;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.IntStream;

public class breadAgent extends Agent {
    // The catalogue of supply items (maps the title name to its quantities)
    ArrayList<calcMethod.supplierInfo> sellingProductList = new ArrayList<>();
    calcMethod supplierInfo = new calcMethod();
    int orderTimer = 180000;
    int timePeriod = 0;
    int ingradPolicy = 1;

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


            // Add the behaviour serving queries from buyer agents
            //addBehaviour(new OfferRequestsServer());

            //Sending random data to specialist
            //Random ingredients data.
            
            //int ingradPolicy = supplierInfo.getRandIntRange(4,6);

            
            sellingProductList.add(supplierInfo.randSupplierInput(getLocalName(),"WhiteBread", "A",ingradPolicy));
            sellingProductList.add(supplierInfo.randSupplierInput(getLocalName(),"WhiteBread", "B",ingradPolicy));
            sellingProductList.add(supplierInfo.randSupplierInput(getLocalName(),"WhiteBread", "C",ingradPolicy));

            //auto update stock which follow the updateProduct method(OneShotBehaviour).
            for (int i=0; i < sellingProductList.size();i++){
                //System.out.println(sellingProductList.get(i).toUpdateService());
                updateProduct(getLocalName(),sellingProductList.get(i).productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
            }


            //Add a TickerBehaviour to refill supply.
            addBehaviour(new TickerBehaviour(this, orderTimer){
                protected void onTick() {

                    //adding the one shoot behaviour to check the ingredient stock in database.
                    addBehaviour(new breadAgent.stopDeliverIngrad());

                    System.out.println("Process after 1 minute");
                    if(ingradPolicy == 0){
                        try {
                            Thread.sleep(orderTimer);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }else {
                            //System.out.println("Process before activate");
                        int dayCountIncomming = orderTimer/60000;
                        timePeriod = timePeriod + dayCountIncomming;

                        for (int i=0; i < sellingProductList.size();i++){
                            //System.out.println(sellingProductList.get(i).toUpdateService());
                            updateProduct(getLocalName(),sellingProductList.get(i).productName, sellingProductList.get(i).ingredientGrade, sellingProductList.get(i).numOfstock);
                        }
                        ingradPolicy = 0;
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
                LocalDate AddedToStock = java.time.LocalDate.now().plusDays(timePeriod);

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

            }
        } );
    }

    private class stopDeliverIngrad extends OneShotBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null && msg.getConversationId().equals("Supplier")){
                myAgent.doSuspend();
            }else {
                block();
            }
        }
    }

    /**
     Inner class OfferRequestsServer.
     This is the behaviour used by supplier agents to serve incoming requests from customer agents.
     The agent replies with a PROPOSE message specifying the price. Otherwise a REFUSE message is
     sent back.
     */
    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

    /**
     Inner class PurchaseOrdersServer.
     This is the behaviour used by Book-seller agents to serve incoming
     offer acceptances (i.e. purchase orders) from buyer agents.
     The seller agent removes the purchased book from its catalogue
     and replies with an INFORM message to notify the buyer that the
     purchase has been sucesfully completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

}