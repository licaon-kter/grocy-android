package xyz.zedler.patrick.grocy.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

@Database(
        entities = {
                ShoppingList.class,
                ShoppingListItem.class,
                ProductGroup.class,
                QuantityUnit.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase { // TODO: set exportSchema
    private static AppDatabase INSTANCE;

    public abstract ShoppingListDao shoppingListDao();

    public abstract ShoppingListItemDao shoppingListItemDao();

    public abstract ProductGroupDao productGroupDao();

    public abstract QuantityUnitDao quantityUnitDao();

    public static AppDatabase getAppDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "app_database"
            ).allowMainThreadQueries().build(); // TODO: No main thread queries!
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
