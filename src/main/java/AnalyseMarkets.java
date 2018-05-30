import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by harry on 2/01/2018.
 */
public class AnalyseMarkets {

    String dataURL = "https://bittrex.com/api/v1.1/public/getmarketsummary?market=";

    HashMap<Integer, Coin> coins;
    int numCoins = 0;
    double profit, dayProfit;
    double gained;
    int coinsBought = 0, coinsHodl = 0, coinsSold = 0, soldLoss = 0, soldProf = 0, day = 1, totalSold = 0, dayCounter = 0;
    double averageLoss = 0, averageProf = 0, highProf = 0, highLoss = 0, volumeTraded = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AnalyseMarkets(){
        coins = new HashMap<>();
    }

    public void init(){


        JSONArray resultArray = null;

        while(resultArray == null) {

            resultArray = getMarketJson();

            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject marketObj = resultArray.getJSONObject(i);
                if (marketObj.getString("MarketName").contains("BTC-")) {
                    coins.put(i, new Coin(marketObj.getString("MarketName")));
                    numCoins++;
                    }
                }
            }

            /*
            for(int i = 0; i < numCoins; i++){
                String ccName = coins.get(i).getName().replaceAll("BTC-","");
                JSONArray ccArray = getCCJSON(ccName);
                for(int j = 0; j < ccArray.length(); j++){
                    JSONObject price = ccArray.getJSONObject(j);
                    double lastPrice = price.getDouble("close");
                    if(lastPrice != Double.NaN){
                        coins.get(j).addPrice(lastPrice, Double.NaN, Double.NaN, false);
                    }
                }
            }
            */

        analyseMarkets();
    }

    private void analyseMarkets(){

        for(Coin coin: coins.values()){
            JSONArray coinData = getCCJSON(coin.getName().replaceAll("BTC-",""));

            for(int i = 0; i < coinData.length(); i++){
                JSONObject price = coinData.getJSONObject(i);
                double lastPrice = price.getDouble("close");
                gained = coin.addPrice(lastPrice, Double.NaN, Double.NaN, true);

                if(!Double.isNaN(gained)){
                    if(gained > 100){
                        coinsBought++;
                        coinsHodl++;
                    } else {
                        coinsSold++;
                        totalSold++;
                        coinsHodl--;
                        //unrealistic, sometimes cryptocompare has whack prices for new coins or outliers
                        //Dont add to data
                        if(gained > 0.01){
                            continue;
                        }
                        volumeTraded += 0.001;
                        profit += gained;
                        dayProfit += gained;

                        if(gained < 0.00000000){
                            if(gained < highLoss){
                                highLoss = gained;
                            }
                            soldLoss++;
                            averageLoss += gained;
                        } else {
                            if(gained > highProf){
                                highProf = gained;
                            }
                            soldProf++;
                            averageProf += gained;
                        }
                    }

                }
            }
        }

        System.out.println("---------- REPORT FOR PERIOD " + day + "----------");
        System.out.println("Total Profit/Loss = " + profit);
        System.out.println("Average Profit/Loss = " + profit/totalSold);
        System.out.println("Volume Traded = " + volumeTraded);
        System.out.println("-------------------------------------");
        System.out.println("Coins Bought today = " + coinsBought);
        System.out.println("Coins sold today = " + coinsSold);
        System.out.println("Coins still Hodl = " + coinsHodl);
        System.out.println("-------------------------------------");
        System.out.println("Profit/Loss for day =  " + dayProfit);
        System.out.println("Average Profit/Loss for day = " + dayProfit/coinsSold);
        System.out.println("-------------------------------------");
        System.out.println("Average Profit = " + averageProf/soldProf);
        System.out.println("Coins sold with Profit = " + soldProf);
        System.out.println("Biggest Profit = " + highProf);
        System.out.println("-------------------------------------");
        System.out.println("Average Loss = " + averageLoss/soldLoss);
        System.out.println("Coins sold with Loss = " + soldLoss);
        System.out.println("Biggest Loss = " + highLoss);
        System.out.println("-------------------------------------");
        System.out.println("");


        /*
                ALL THIS BELOW IS TO ACTUALLY RUN THE BOT ON BITTREX
         */
        /**
        final Runnable printReport = new Runnable() {
            @Override
            public void run() {
                System.out.println("---------- REPORT FOR PERIOD " + day + "----------");
                System.out.println("Total Profit/Loss = " + profit);
                System.out.println("Average Profit/Loss = " + profit/totalSold);
                System.out.println("-------------------------------------");
                System.out.println("Coins Bought today = " + coinsBought);
                System.out.println("Coins sold today = " + coinsSold);
                System.out.println("Coins still Hodl = " + coinsHodl);
                System.out.println("-------------------------------------");
                System.out.println("Profit/Loss for day =  " + dayProfit);
                System.out.println("Average Profit/Loss for day = " + dayProfit/coinsSold);
                System.out.println("-------------------------------------");
                System.out.println("Average Profit = " + averageProf/soldProf);
                System.out.println("Coins sold with Profit = " + soldProf);
                System.out.println("Biggest Profit = " + highProf);
                System.out.println("-------------------------------------");
                System.out.println("Average Loss = " + averageLoss/soldLoss);
                System.out.println("Coins sold with Loss = " + soldLoss);
                System.out.println("Biggest Loss = " + highLoss);
                System.out.println("-------------------------------------");
                System.out.println("");

                day++;
                dayProfit = 0; coinsSold = 0; coinsBought = 0; highLoss = 0; highProf = 0; soldLoss = 0; soldProf = 0; averageLoss = 0; averageProf = 0;

            }
        };

        final Runnable gatherData = new Runnable() {
            @Override
            public void run() {

                JSONArray resultArray = getMarketJson();

                outer:
                for(int j = 0; j < numCoins; j++){
                    JSONObject marketObj = resultArray.getJSONObject(j);
                    for(int k = 0; k < numCoins; k++){
                        if(marketObj.getString("MarketName").equalsIgnoreCase(coins.get(k).getName())){

                            gained = coins.get(k).addPrice(marketObj.getDouble("Last"), marketObj.getDouble("Bid"), marketObj.getDouble("Ask"), true);

                            if(!Double.isNaN(gained)){
                                if(gained > 100.0){
                                    coinsBought++;
                                    coinsHodl++;
                                } else {
                                    coinsHodl--;
                                    profit += gained;
                                    dayProfit += gained;
                                    coinsSold++;
                                    totalSold++;
                                    if(gained < 0.00000000){
                                        if(gained < highLoss){
                                            highLoss = gained;
                                        }
                                        soldLoss++;
                                        averageLoss += gained;
                                    } else {
                                        if(gained > highProf){
                                            highProf = gained;
                                        }
                                        soldProf++;
                                        averageProf += gained;
                                    }
                                }

                            }
                            continue outer;
                        }
                    }
                }
            }
        };

         //CHANGE THE TWO NUMBERS IN THE PARAMETERS TO SUIT YOUR HOURLY OR DAILY INTERVAL
        final ScheduledFuture<?> dataHandler = scheduler.scheduleAtFixedRate(gatherData, 72, 72, TimeUnit.HOURS);
        final ScheduledFuture<?> reportHandler = scheduler.scheduleAtFixedRate(printReport, 72, 72, TimeUnit.HOURS);
        **/
    }

    //GETS MARKETSUMMARIES FROM BITTREX,HAS A LOT OF INFO FOR EACH COIN, USE THE URL TO SEE, ONLY CURRENT PRICES NOT PRICE HISTORY
    private JSONArray getMarketJson(){
        try{
            URL url = new URL("https://bittrex.com/api/v1.1/public/getmarketsummaries");
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF8"));

            StringBuilder response = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null){
                response.append(line);
            }

            bufferedReader.close();

            JSONObject obj = new JSONObject(response.toString());
            Boolean success = obj.getBoolean("success");
            if(success) {
                JSONArray resultArray = obj.getJSONArray("result");
                return resultArray;
            } else {
                return null;
            }
        } catch (IOException e){

            System.out.println("ERROR getting JSON: " + e.getMessage());
            return getMarketJson();
        }
    }

    //GETS PRICE AND VOLUME HISTORY FOR A SINGLE COIN, CAN BE EVERY MINUTE, HOUR OR DAY AND CAN GET UPTO 2000 RECORDS, EG 2000 HOURS
    private JSONArray getCCJSON(String name){
        try{
            //CHANGE HERE TO SET TIME INTERVAL AND AMOUNT OF TIMES ----histohour, histominute-----------------ex:365 days intv---1hr, 3hr, 15hr, 1day ect----
            //Currently set up to run over the last 120 days for every 6 hours
            URL url = new URL("https://min-api.cryptocompare.com/data/histohour?fsym="+name+"&tsym=BTC&limit=480&aggregate=12&e=CCCAGG");
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF8"));

            StringBuilder response = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null){
                response.append(line);
            }

            bufferedReader.close();

            JSONObject obj = new JSONObject(response.toString());

            JSONArray resultArray = obj.getJSONArray("Data");
            return resultArray;
        } catch (IOException e){
            System.out.println("ERROR getting JSON: " + e.getMessage());
            return null;
        }
    }


}
