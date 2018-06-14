package TradeCenter;

import Interface.searchCard.filterChoice.PokemonAll;
import TradeCenter.Card.Card;
import TradeCenter.Card.CardCatalog;

import TradeCenter.Card.PokemonDescription;
import TradeCenter.Exceptions.CardExceptions.CardNotFoundException;
import TradeCenter.Exceptions.CardExceptions.EmptyCollectionException;
import TradeCenter.Exceptions.TradeExceptions.AlreadyStartedTradeException;
import TradeCenter.Exceptions.TradeExceptions.MyselfTradeException;
import TradeCenter.Exceptions.TradeExceptions.NoSuchDescriptionFoundedException;
import TradeCenter.Exceptions.TradeExceptions.NoSuchTradeException;
import TradeCenter.Exceptions.UserExceptions.CheckPasswordConditionsException;
import TradeCenter.Exceptions.UserExceptions.UserNotFoundException;
import TradeCenter.Card.Description;
import TradeCenter.Exceptions.UserExceptions.UsernameAlreadyTakenException;
import TradeCenter.Trades.*;
import TradeCenter.Customers.*;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Class representing the trading center.
 *
 * It keeps a list of customers that interacts with each other
 * Customers have their own collection, taken from a database
 */
public class TradeCenter {

    private int contUsers;
    private CardCatalog pokemonCatalog;
    private CardCatalog yugiohCatalog;
    private HashMap<String, Customer> customers;
    private ArrayList<Trade> activeTrades;
    private ArrayList<Trade> doneTrades;
    private DBProxy proxy;

    /**
     * Create a new trade center, with the database connection that load cards and users with their attributes
     */
    public TradeCenter() {
        this.contUsers = 0;
        this.proxy = new DBProxy();
        this.pokemonCatalog = new CardCatalog();
        this.yugiohCatalog = new CardCatalog();
        proxy.populateCatalog("pokemon_card", pokemonCatalog);
        proxy.populateCatalog("yugioh_card", yugiohCatalog);
        this.customers = new HashMap<String, Customer>();
        contUsers = proxy.retrieveCustomers(customers);
        this.activeTrades = new ArrayList<Trade>();
        this.doneTrades = new ArrayList<Trade>();
    }
    /**
     * Create an account for a new user
     */
    public void addCustomer(String username, String password) throws CheckPasswordConditionsException{
        if(usernameTaken(username)){
            throw new UsernameAlreadyTakenException();
        }else{
            String id = customerID();
            Customer temporaryCustomer = new Customer(id, username, password);
            customers.put(id, temporaryCustomer);
            proxy.insertCustomer(temporaryCustomer);
        }
    }


    /**
     * A method that check if the username is already in use
     *
     * @param username
     * @return a boolean value
     */
    private boolean usernameTaken(String username){
        for(String key : customers.keySet()){
            if(customers.get(key).getUsername().equals(username)){
                return true;
            }

        }
        return false;
    }

    /**
     * Create a unique ID for the user
     *
     * @return the ID
     */
    private String customerID(){
        contUsers++;
        return "USER-" + contUsers;
    }

    /**
     * A method that verify if the passwords are the same when singin-up
     * @param password1 the password
     * @param password2 check for first password
     * @return if the password are equals
     */
    public boolean verifyPassword(String password1, String password2){
        return password1.equals(password2);
    }

    /**
     * A method that checks if the user's password is correct
     * @param username
     * @param password
     * @return if the login ended in a correct way
     */
    public boolean loggedIn(String username, String password){
        Customer customer;
        try{
            customer = searchCustomer(username);
        }catch(UserNotFoundException e){
            customer = null;
            e.printStackTrace();
        }
        if(customer == null){
            //todo username inesistente fare registrare
            return false;
        }
        if(customer.getUsername().equals(username) && customer.checkPassword(password)){
            return true;
        }else{
            //usere registrato ma password sbagliata //todo richiedere password
            return false;
        }

    }


    //todo add javadocs, check if the customer exist,
    public void addCardtoCustomer(Customer customer, Card card){
        customer.addCard(card);
        proxy.updateCustomer(customer);
    }

    /**
     * A method that removes a card from a Customer Wishlist
     * @param cardDescription the card to remove
     * @param customer the customer that wants to remove the card
     */
    public void removeFromWishList(Description cardDescription, Customer customer) {

        customer.removeFromWishList(cardDescription);
        //todo fare update con proxy
    }

    /**
     * A Method that updates the customer's attributes, like wishlist
     * @param customer the user that has to be updated
     */
    private void updateCustomer(Customer customer){
        proxy.updateCustomer(customer);
    }

    /**
     * User can find another user by searching his name
     * @return
     */
    public Customer searchCustomer(String username){
        for(String key : customers.keySet()){
            if((customers.get(key)).getUsername().equals(username)){
                return customers.get(key);
            }
        }
        //user not found
        throw new UserNotFoundException();
    }

    /**
     * A method that search all the customers that have a username similar to a string searched
     * @param username the string for the research
     * @param myUsername a string to exclude myself from the research
     * @return the list of customers that corresponds to the search
     */
    public ArrayList<Customer> searchUsers(String username, String myUsername){
        ArrayList<Customer> results = new ArrayList<>();
        String usernameLow = username.toLowerCase();
        for(String key: customers.keySet()){
            String name = customers.get(key).getUsername().toLowerCase();
            if(name.contains(usernameLow) && !customers.get(key).getUsername().equals(myUsername)){
                results.add(customers.get(key));
            }
        }
        return results;
    }

    /**
     *  A method that checks if any of the user has a given card
     * @param description the card that has to be searched
     * @return the list of the users that have that card in their collection
     */
    public ArrayList<HashMap<Customer, Collection>> searchByDescription(Description description){
        ArrayList<HashMap<Customer, Collection>> cards = new ArrayList<HashMap<Customer, Collection>>();

        for(String key: customers.keySet()){
            HashMap<Customer, Collection> customerCollection = customers.get(key).searchByDescription(description);
                if(customerCollection!=null) {
                    cards.add(customerCollection);
                }

        }

        return cards;
    }

    /**
     *
     * @param pokemonAll
     * @return
     */
    public HashSet<PokemonDescription> searchDescrInPokemonDb(PokemonAll pokemonAll) throws NoSuchDescriptionFoundedException {

        String type=pokemonAll.getType();
        int hp=pokemonAll.getHp();
        int lev=pokemonAll.getLev();
        int weigth=pokemonAll.getWeigth();
        String len1=pokemonAll.getLen1();
        String len2=pokemonAll.getLen2();
        HashSet<PokemonDescription> descrMatched=new HashSet<>();
        descrMatched=proxy.getSearchedDescrPokemon(type,hp,lev,weigth,len1,len2);
        /*if(descrMatched.size()==0)
            throw new NoSuchDescriptionFoundedException();
            */
        return descrMatched;
    }

    /**
     *
     * @return
     */
    public HashSet<Description> searchDescrInYugiohDb(){
        HashSet<Description> descrMatched=new HashSet<>();
        return descrMatched;
    }

    /**
     * Users can search a card, they see all users that match the search
     *
     * @param searchString name or description of a card
     * @return a list of customers with their own collections
     */
    public HashMap<Customer, Collection> searchByString(String searchString){
        HashMap<Customer, Collection> searched = new HashMap<>();
        for(String key : customers.keySet()){
            searched.put(customers.get(key), customers.get(key).searchByString(searchString));
        }
        if(searched.size() == 0){
            //nothing found
            throw new CardNotFoundException();          //todo eccezione da risolvere, se nessuno ha la carta il software deve continuare
        }                                               //todo PARLARE CON FEDE
        return searched;
    }

    //todo add javadocs
    public void createTrade(Customer customer1, Customer customer2, Collection offer1, Collection offer2){
        if(notAlreadyTradingWith(customer1, customer2)){
            try {
                Trade Trade = new Trade(new Offer(customer1, customer2, offer1, offer2));
                activeTrades.add(Trade);

                //todo vedere se si deve fare update con il db
            }catch (MyselfTradeException e){
                System.err.println(e.getMessage());
            }catch(EmptyCollectionException e){
                System.err.println(e.getMessage());
            }
        }else{
            throw new AlreadyStartedTradeException(customer2.getUsername());

            //todo propagar eccezione all'interfaccia tramite socket
        }
    }

    //todo add javadocs
    public void updateTrade(Offer offer){
        Trade trade = takeStartedTrade(offer.getCustomer1(), offer.getCustomer2());
        trade.update(offer);
    }

    /**
     * A method that checks if two users are already trading
     *
     * @param myCustomer customer1
     * @param otherCustomer customer2
     * @return if the user are already trading
     */
    public boolean notAlreadyTradingWith(Customer myCustomer, Customer otherCustomer){
        boolean flag = true;
        for(Trade trade: activeTrades){
            if(trade.betweenUsers(myCustomer.getUsername()) && trade.betweenUsers(otherCustomer.getUsername())){
                //todo vedere quando passa se stesso con se stesso, non si puo creare a monte
                flag = false;
            }
        }
        return flag;
    }

    //todo add javadocs
    public Trade takeStartedTrade(Customer myCustomer, Customer otherCustomer){
        Trade searchTrade = null;
        for(Trade trade : activeTrades){
            if(trade.betweenUsers(myCustomer.getUsername()) && trade.betweenUsers(otherCustomer.getUsername())){
                searchTrade = trade;
            }
        }
        return searchTrade;
    }

    private boolean contains(Trade trade){
        for (Trade trade1 : activeTrades){
            if(trade1.betweenUsers(trade.getCustomer1().getUsername()) || trade1.betweenUsers(trade.getCustomer2().getUsername())){
                return true;
            }
        }

        return false;
    }

    /**
     * Do the exchange, users collection are update, and so the trade
     *
     * @param trade a card exchange
     */
    private void switchCards(Trade trade){
        if(contains(trade)) {

            for (Card card : trade.getOffer1()) {       //take card offered from customer1, add to customer2
                customers.get(trade.getCustomer2().getId()).addCard(card);
            }
            for (Card card : trade.getOffer2()) {       //take card offered from customer2, add to customer1
                customers.get(trade.getCustomer1().getId()).addCard(card);
            }
            for(Card card : trade.getOffer1()){
                customers.get(trade.getCustomer1().getId()).removeCard(card);
            }
            for(Card card : trade.getOffer2()){
                customers.get(trade.getCustomer2().getId()).removeCard(card);
            }
            proxy.updateCustomer(customers.get(trade.getCustomer1().getId()));
            proxy.updateCustomer(customers.get(trade.getCustomer2().getId()));
            //todo vedere se serve fare update proxy
        }else{
            throw new NoSuchTradeException();
        }
    }

    public void endTrade(Trade trade, boolean result){
        if(result){
            switchCards(trade);
        }
        activeTrades.remove(trade);     //deve essere cosi l'ordine altrimenti trade != trade negli active trade
        trade.doneDeal(result);
        doneTrades.add(trade);
    }

    /**
     * Check for deals that are over, if so it updates the list of active and done deals
     * Also if a deal it's over, the exchange is done
     */
    /*per ora inutile viene tutto fatto nel metodo switchcards
    public void checkDoneDeals(){
        for(Trade trade : activeTrades){        //todo togliere ciclo, ricontrollare per sincronia con interfaccia grafica e db
            if(trade.isDoneDeal()){
                switchCards(trade);
                doneTrades.add(trade);
                activeTrades.remove(trade);
            }
        }
    }

    */

    /**
     * A method that shows a log of all the trades that ended
     */
    public void logDoneTrades(){
        for (Trade trade : doneTrades){
            System.out.println(trade);
        }
    }

    /**
     * A method that shows the trades still active of a customer
     *
     * @param customer the customer that wants to see his trades
     * @return the list of the customer's trades
     */
    private ArrayList<Trade> showUserActiveTrades(Customer customer){
        ArrayList<Trade> activeTradeList = new ArrayList<>();
        for(Trade trade : activeTrades){
            if(trade.betweenUsers(customer.getUsername())){
                activeTradeList.add(trade);
            }
        }
        return activeTradeList;
    }

    /**
     * A method that shows the finished trades of a customer
     *
     * @param customer the customer that wants to see his trades
     * @return the list of the customer's trades
     */
    private ArrayList<Trade> showUserDoneTrades(Customer customer){
        ArrayList<Trade> doneTradesList = new ArrayList<>();
        for(Trade trade : doneTrades){
            if(trade.betweenUsers(customer.getUsername())){
                doneTradesList.add(trade);
            }
        }
        return doneTradesList;
    }

    /**
     * A method that shows all the trades of a user
     *
     * @param customer the user
     * @return the list of trades of the user, first the active ones
     */
    public ArrayList<Trade> showUserTrades(Customer customer){
        ArrayList<Trade> tradesList = new ArrayList<>();
        tradesList.addAll(showUserActiveTrades(customer));
        tradesList.addAll(showUserDoneTrades(customer));
        return tradesList;
    }

}
