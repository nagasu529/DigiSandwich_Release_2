import jade.Boot;

public class Main {
    public static void main(String[] args){
        int customerSize = 10;
        StringBuilder customerStartText = new StringBuilder();
        for (int i = 0; i < customerSize; i++){
            //String tempText = String.format("customer%d:agent.raynorCustomerAgent;",i + 1);       //Row data from Raynor's
            String tempText = String.format("customer%d:agent.customerRandomAgent;",i + 1);
            customerStartText.append(tempText);
        }

        String[] param = new String[2];
        param[0] = "-gui";
        param[1] = " specialist:agent.specialistAgent;supplier:agent.supplierAgent;" + customerStartText;
        //param[2] = String.valueOf(customerStartText);
        Boot.main(param);
    }
}
