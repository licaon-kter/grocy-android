/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMulti;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.repository.MasterObjectListRepository;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class MasterObjectListViewModel extends BaseViewModel {

  private static final String TAG = MasterObjectListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterObjectListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<Object>> displayedItemsLive;

  private List<?> objects;
  private List<ProductGroup> productGroups;
  private List<QuantityUnit> quantityUnits;
  private List<Location> locations;

  private final HorizontalFilterBarMulti horizontalFilterBarMulti;
  private boolean sortAscending;
  private String search;
  private final boolean debug;
  private final String entity;

  public MasterObjectListViewModel(@NonNull Application application, String entity) {
    super(application);

    this.entity = entity;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterObjectListRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    displayedItemsLive = new MutableLiveData<>();

    objects = new ArrayList<>();

    horizontalFilterBarMulti = new HorizontalFilterBarMulti(this::displayItems);
    sortAscending = true;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      switch (entity) {
        case ENTITY.PRODUCTS:
          this.objects = data.getProducts();
          this.productGroups = data.getProductGroups();
          this.quantityUnits = data.getQuantityUnits();
          this.locations = data.getLocations();
          break;
        case ENTITY.PRODUCT_GROUPS:
          this.objects = data.getProductGroups();
          break;
        case ENTITY.LOCATIONS:
          this.objects = data.getLocations();
          break;
        case ENTITY.QUANTITY_UNITS:
          this.objects = data.getQuantityUnits();
          break;
        case ENTITY.TASK_CATEGORIES:
          this.objects = data.getTaskCategories();
          break;
        default:
          this.objects = data.getStores();
          break;
      }

      displayItems();
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        }, error -> onError(error, TAG),
        forceUpdate,
        true,
        entity.equals(GrocyApi.ENTITY.STORES) ? Store.class : null,
        (entity.equals(GrocyApi.ENTITY.LOCATIONS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
            ? Product.class : null,
        (entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
            ? ProductGroup.class : null,
        (entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
            ? QuantityUnit.class : null,
        entity.equals(ENTITY.TASK_CATEGORIES) ? TaskCategory.class : null,
        entity.equals(GrocyApi.ENTITY.PRODUCTS) ? Product.class : null
    );
  }

  public void displayItems() {
    // search items
    ArrayList<Object> searchedItems;
    if (search != null && !search.isEmpty()) {

      ArrayList<Object> searchResultsFuzzy = new ArrayList<>(objects.size());
      List results = FuzzySearch.extractSorted(
          search,
          objects,
          item -> {
            String name = ObjectUtil.getObjectName(item, entity);
            return name != null ? name.toLowerCase() : "";
          },
          70
      );
      for (Object result : results) {
        searchResultsFuzzy.add(((BoundExtractedResult<?>) result).getReferent());
      }

      searchedItems = new ArrayList<>();
      ArrayList<Integer> objectIdsInList = new ArrayList<>();
      for (Object object : objects) {
        String name = ObjectUtil.getObjectName(object, entity);
        name = name != null ? name.toLowerCase() : "";
        if (name.contains(search)) {
          searchedItems.add(object);
          objectIdsInList.add(ObjectUtil.getObjectId(object, entity));
        }
      }

      sortObjectsByName(searchedItems);

      for (Object object : searchResultsFuzzy) {
        if (objectIdsInList.contains(ObjectUtil.getObjectId(object, entity))) {
          continue;
        }
        searchedItems.add(object);
      }
    } else {
      searchedItems = new ArrayList<>(objects);
      sortObjectsByName(searchedItems);
    }

    // filter items
    ArrayList<Object> filteredItems;
    if (entity.equals(GrocyApi.ENTITY.PRODUCTS) && horizontalFilterBarMulti.areFiltersActive()) {
      filteredItems = new ArrayList<>();
      HorizontalFilterBarMulti.Filter filter = horizontalFilterBarMulti
          .getFilter(HorizontalFilterBarMulti.PRODUCT_GROUP);
      for (Object object : searchedItems) {
        if (!NumUtil.isStringInt(((Product) object).getProductGroupId())) {
          continue;
        }
        int productGroupId = Integer.parseInt(((Product) object).getProductGroupId());
        if (productGroupId == filter.getObjectId()) {
          filteredItems.add(object);
        }
      }
    } else {
      filteredItems = searchedItems;
    }

    displayedItemsLive.setValue(filteredItems);
  }

  public void sortObjectsByName(ArrayList<Object> objects) {
    if (objects == null) {
      return;
    }

    Locale locale = LocaleUtil.getLocale();

    Collections.sort(objects, (item1, item2) -> {
      String name1 = ObjectUtil.getObjectName(sortAscending ? item1 : item2, entity);
      String name2 = ObjectUtil.getObjectName(sortAscending ? item2 : item1, entity);
      if (name1 == null || name2 == null) {
        return 0;
      }

      return Collator.getInstance(locale).compare(
              name1.toLowerCase(locale),
              name2.toLowerCase(locale));
    });
  }

  public void showProductBottomSheet(Product product) {
    if (product == null) {
      return;
    }
    Bundle bundle = new ProductOverviewBottomSheetArgs.Builder()
        .setProduct(product)
        .setLocation(Location.getFromId(locations, product.getLocationIdInt()))
        .setQuantityUnitPurchase(QuantityUnit.getFromId(quantityUnits, product.getQuIdPurchaseInt()))
        .setQuantityUnitStock(QuantityUnit.getFromId(quantityUnits, product.getQuIdStockInt()))
        .setShowActions(false)
        .build().toBundle();
    showBottomSheet(new ProductOverviewBottomSheet(), bundle);
  }

  public void deleteObject(int objectId) {
    dlHelper.delete(
        grocyApi.getObject(entity, objectId),
        response -> downloadData(false),
        error -> showMessage(getString(R.string.error_undefined))
    );
  }

  @Nullable
  public List<ProductGroup> getProductGroups() {
    return productGroups;
  }

  public void setSortAscending(boolean ascending) {
    this.sortAscending = ascending;
    displayItems();
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  public boolean isSearchActive() {
    return search != null;
  }

  public void setSearch(@Nullable String search) {
    this.search = search != null ? search.toLowerCase() : null;
    displayItems();
  }

  public void deleteSearch() {
    search = null;
  }

  public HorizontalFilterBarMulti getHorizontalFilterBarMulti() {
    return horizontalFilterBarMulti;
  }

  @NonNull
  public MutableLiveData<ArrayList<Object>> getDisplayedItemsLive() {
    return displayedItemsLive;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MasterObjectListViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final String entity;

    public MasterObjectListViewModelFactory(Application application, String entity) {
      this.application = application;
      this.entity = entity;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterObjectListViewModel(application, entity);
    }
  }
}
