package io.xpipe.sample;


import io.xpipe.api.DataSource;
import io.xpipe.api.DataTable;
import io.xpipe.core.data.node.TupleNode;

import java.util.Comparator;
import java.util.Map;

public class HomePricesSample {

    private static DataTable homePricesTable;

    public static void main(String[] args) {
        var resource = HomePricesSample.class.getResource("homes.csv");

        // Creates a wrapped data source using the url.
        // Note that while this is possible, it is not recommended as
        // all queries are routed through the XPipe client anyway.
        // It allows us however to bundle the data with this sample program.
        homePricesTable = DataSource.createAnonymous("csv", Map.of(), resource).asTable();

        // As we didn't pass any configuration parameters, X-Pipe will try to automatically detect
        // the correct configuration parameters. You can access these parameters like this:
        System.out.println("Determined configuration: " + homePricesTable.getConfig());
        // In case these some parameters are not chosen correctly, you can pass the proper values
        // to the wrap() method.

        System.out.println("The highest selling house entry is: " + getHighestSellingHouse());
    }

    private static TupleNode getHighestSellingHouse() {
        return homePricesTable.stream()
                .min(Comparator.comparingInt(t -> t.forKey("Sell").asInt()))
                .get();
    }
}
