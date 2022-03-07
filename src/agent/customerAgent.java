package agent;

import database.DatabaseConn;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import agent.calcMethod.customerInfo;

public class customerAgent extends Agent {
    //customerUI myGui;

    // Put agent initializations here
    calcMethod customerInfo = new calcMethod();
    DatabaseConn app = new DatabaseConn();
    //calcMethod.customerInfo randInput = customerInfo.randCustomerInput(getLocalName());
    calcMethod.customerInfo weeklyOrder = customerInfo. new customerInfo(getLocalName(),"Hamsandwich","general",100,app.selectProductPrice("Hamsandwich","general"),0,0,0);
    int orderTimer = 0;
    int[] orderTimerArray = {60000,180000,300000,4200000};

    //calcMethod.customerInfo randInput = customerInfo.customerInfo(getLocalName(), " ", " ", 0, 0, 0, "  ", 0);

    protected void setup() {

    	// Register service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("customer");
        sd.setName(getAID().getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    	
        //put agent name to ArrayList
        //randInput.agentName = getLocalName();
        //Timing for agent environment
        orderTimer = orderTimerArray[orderTimerArray.length -1];
        //orderTimer = orderTimerArray[customerInfo.getRandIntRange(0, orderTimerArray.length - 1)];
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //GUI active
        //myGui = new customerUI(this);
        //myGui.show();

        //myGui.displayUI(getLocalName());
        //System.out.println(randInput.toUpdateService());
        // Printout a welcome message

        //System.out.println("Hello! Customer-agent " + getAID().getName() + " is ready.");
        // Customer information detail
        
        addBehaviour(new customerAgent.ReceivedOrderRequest());

        addBehaviour(new TickerBehaviour(this, orderTimer){
            protected void onTick() {
                orderTimer = orderTimerArray[customerInfo.getRandIntRange(0, orderTimerArray.length - 1)];
                //update current stock on list to suppliers.
                //addBehaviour(new responseToCustomers());
                addBehaviour(new customerAgent.RequestPerformer());
            }
        } );

    }

    // Put agent to request sandwich here
    protected void takeDown() {
        // Printout a dismissal message
        //myGui.dispose();
        //System.out.println("Customer-agent "+getAID().getName()+" terminating.");
    }

    private class RequestPerformer extends OneShotBehaviour{
        private AID[] specialistAgent; //The specialist location contain
        private MessageTemplate mt; // The template to receive replies
        //Updating the global specialistAddress

        public void action() {
            //Searching for current specialist.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdSearch = new ServiceDescription();
            sdSearch.setType("specialist");
            template.addServices(sdSearch);
            try {
                DFAgentDescription[] searchResult = DFService.search(myAgent, template);
                specialistAgent = new AID[searchResult.length];
                for (int i = 0; i < searchResult.length; ++i) {
                    specialistAgent[i] = searchResult[i].getName();
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (int j = 0; j < specialistAgent.length; j++) {
                cfp.addReceiver(specialistAgent[j]);
            }
            cfp.setContent(weeklyOrder.toUpdateService());
            cfp.setConversationId("customer");
            cfp.setReplyWith("cfp" + System.currentTimeMillis());
            myAgent.send(cfp);
            //System.out.println(cfp);

            // Prepare the template to get proposals
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("buying-request"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
            //myGui.displayUI("Sending request to buy message to specialist:");
            //myGui.displayUI("Request order: " + randInput.toStringOutput());

        }
    }

    private class ReceivedOrderRequest extends CyclicBehaviour {
        public void action() {
            MessageTemplate mtAccept = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            MessageTemplate mtReject = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);

            ACLMessage accMsg = myAgent.receive(mtAccept);
            ACLMessage rejMsg = myAgent.receive(mtReject);

            if (accMsg != null && accMsg.getConversationId().equals("reply-to-customer")) {
                //myGui.displayUI(accMsg.toString());
                String tempContent = accMsg.getContent();
                String[] arrOfStr = tempContent.split("-");
                int numReplyFromSpeciailst = Integer.parseInt(arrOfStr[4]);
                int replyStatus = Integer.parseInt(arrOfStr[5]);
                weeklyOrder.numReply = numReplyFromSpeciailst;
                weeklyOrder.orderStatus = replyStatus;

                //Date and time testing.
                LocalDate A = java.time.LocalDate.now();
                LocalDate expired = java.time.LocalDate.now().plusDays(21);
                long p = ChronoUnit.DAYS.between(A,expired);
                int a  = Integer.parseInt(String.valueOf(p));

                Period period = Period.between(A, expired);
                //myGui.displayUI(period.toString());
                //myGui.displayUI(expired.toString());

                //Updating agent status (dispose or re-send request)
                if(weeklyOrder.numReply == weeklyOrder.numOfOrder){
                    //myGui.displayUI("Received all order requirement");
                    //myAgent.doSuspend();

                }else {
                    //myGui.displayUI(String.format("\n The reserved order is %d from %d request",randInput.numReply,randInput.numOfOrder));
                    weeklyOrder.numOfOrder = weeklyOrder.numOfOrder - weeklyOrder.numReply;
                    //myAgent.doSuspend();
                    //myGui.displayUI(randInput.toStringOutput());
                }
            }
            if(rejMsg != null && rejMsg.getConversationId().equals("reply-to-customer")){
                //myGui.displayUI("The request order cannot accepted today.... please try again tomorrow");
                //myAgent.doSuspend();
            }
            else {
                block();
            }
        }
    }
}