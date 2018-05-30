import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by harry on 2/01/2018.
 */
public class Coin {

    String name;
    List<Double> last12, last26, last9MACD, sma7day;
    int RSIcount = 0;
    double sma12 = 101, sma26, ema12 = 101, ema26, MACD, signalLine, buyPrice, hodl, gains;
    double avLoss, avGain, RSI;
    boolean wasBelow = false, bullishMACDcross = false;
    boolean wasAbove = false, bearishMACDcross = false;
    boolean hasBought = false;
    boolean holdOff = true;
    boolean tradable = false;
    double bid, ask;



    public Coin(String name){
        this.name = name;
        last12 = new ArrayList<>();
        last26 = new ArrayList<>();
        last9MACD = new ArrayList<>();
        sma7day = new ArrayList<>();
    }

    public double addPrice(Double price, Double bid, Double ask, boolean condition){

        tradable = condition;
        this.bid = bid;
        this.ask = ask;
        last12.add(price);
        last26.add(price);

        if(last12.size() > 12){
            last12.remove(0);
        }

        if(last26.size() > 26) {
            last26.remove(0);

            //These are all calculated but only using MACD crossovers as an indicator
            calculateRSI();
            calculateSMA();
            calculateEMA(price);
            calculateMACD();
            if(!hasBought && buyTrigger()){
                //change price to ask if running on bittrex
                buyCoin(price);
                return 121.01;
            }

            if(hasBought && sellTrigger()){
                //change price to bid if running on bittrex
                sellCoin(price);
                return gains;
            }
        }

        return Double.NaN;
    }

    private void buyCoin(double ask){
        hasBought = true;
        hodl = (0.001 / ask);
        buyPrice = ask;

        /*
        String urlString = "https://bittrex.com/api/v1.1/market/buylimit?" +
                "market=" +name+ "&" +
                "quantity="+hodl+"&"+
                "rate="+ask+ "&" +
                "apikey=ced29be973354cc3816aeb41e199e796&nonce="+EncryptionUtility.generateNonce();

        sendRequest(urlString);
        */
    }

    private void sellCoin(double bid) {
        hasBought = false;
        gains = hodl * bid - 0.001005; // bittrex fee

        /*
        String urlString = "https://bittrex.com/api/v1.1/market/selllimit?" +
                "market=" + name + "&" +
                "quantity=" + hodl + "&" +
                "rate=" + bid + "&" +
                "apikey=ced29be973354cc3816aeb41e199e796&nonce=" + EncryptionUtility.generateNonce();

        sendRequest(urlString);
        */
        }

    public boolean buyTrigger() {
        //if it 7 hr av was previously below 25 hr av and has now crossed the 25 hr av
        //we should buy, set was above boolean to true and the conditions for buying to false;

        if (wasBelow && bullishMACDcross) {
            wasBelow = false;
            bullishMACDcross = false;
            wasAbove = true;
            return true;
        }

        return false;
    }

    public boolean sellTrigger() {
        //if it wasAbove (only set when to true if we bought) and the 25 hr av has gone below the
        // 7 hr av then we should sell, set was below to true as now below and the sell conditions back to false

        if (wasAbove && bearishMACDcross) {
            wasBelow = true;
            bearishMACDcross = false;
            wasAbove = false;
            return true;
        }

        return false;
    }

    private void calculateSMA() {
        double ma12Total = 0, ma26Total = 0;
        for (int i = 0; i < last12.size(); i++) {
            ma12Total += last12.get(i);
        }

        for (int i = 0; i < last26.size(); i++) {
            ma26Total += last26.get(i);
        }

        sma26 = ma26Total / 26;
        sma12 = ma12Total / 12;

    }

    public void calculateEMA(double price) {

        if (ema12 == Double.NaN) {
            ema12 = sma12;
            ema26 = sma26;
            return;
        }

        ema12 = (price - ema12) * 0.1538 + ema12;
        ema26 = (price - ema26) * 0.07407 + ema26;
    }

    private void calculateMACD(){

        MACD = ema12 - ema26;
        last9MACD.add(MACD);

        if (last9MACD.size() > 9) {
            last9MACD.remove(0);

            //Create Initial signal line from the MACD, only happens once
            if (holdOff) {
                for (int i = 0; i < 9; i++) {
                    signalLine += last9MACD.get(i);
                }
                signalLine = signalLine / 9;
                holdOff = false;

            } else {
                signalLine = (MACD - signalLine) * 0.2 + signalLine;

                //If MACD is less below signal line
                if(tradable){
                    if (MACD < signalLine) {
                        wasBelow = true;
                    }

                    //MACD bullish cross over
                    if (MACD > signalLine && wasBelow) {
                        bullishMACDcross = true;
                    }

                    //MACD bearish cross over
                    if (MACD < signalLine && wasAbove) {
                        bearishMACDcross = true;
                    }
                }
            }
        }
    }

    public void calculateRSI() {
        double RS;
        if (RSIcount == 0) {
            for (int i = 1; i < last26.size(); i++) {
                if (last26.get(i) < last26.get(i - 1)) {
                    avLoss = avLoss + (last26.get(i - 1) - last26.get(i));
                } else {
                    avGain = avGain + (last26.get(i) - last26.get(i - 1));
                }
            }
            avLoss = avLoss / 25;
            avGain = avGain / 25;
            RSIcount = 1;
        } else {
            if (last26.get(last26.size() - 1) < last26.get(last26.size() - 2)) {
                avLoss = ((avLoss * 25) + (last26.get(last26.size() - 2) - last26.get(last26.size() - 1))) / 25;
            } else {
                avGain = ((avGain * 25) + (last26.get(last26.size() - 1) - last26.get(last26.size() - 2))) / 25;
            }


        }

        RS = avGain / avLoss;

        RSI = 100 - (100 / (1 + RS));

    }

    public String getName() {
        return name;
    }

    public void sendRequest(String urlString) {
        try{

            String result;
            URL url = new URL(urlString);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setRequestProperty("apisign", EncryptionUtility.calculateHash("3207e05b33b3401f9e078a57746b34c4", urlString, "HmacSHA512"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));

            StringBuffer resultBuffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null)

                resultBuffer.append(line);

            result = resultBuffer.toString();
            System.out.println(result);

        } catch(UnknownHostException | SocketException e) {
            sendRequest(urlString);
            System.out.println("Failed to execute sell");
        } catch(IOException e){
            sendRequest(urlString);
        }
    }

}
