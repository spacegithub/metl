package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;
import org.jumpmind.properties.TypedProperties;

public class Sorter extends AbstractComponentRuntime {

    // TODO: Instead of making the sort attribute a single component level
    // setting
    // make it an attribute setting with the value being the sort order
    // to allow for n number of sort fields in any specific order.
    // Make custom UI to allow drag and drop ordering of the model fields

    public static final String TYPE = "Sorter";

    public final static String SORT_ATTRIBUTE = "sort.attribute";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    int rowsPerMessage;
    
    String sortAttributeId;
    
    List<EntityData> sortedRecords = new ArrayList<EntityData>();

    @Override
    protected void start() {
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (int i = 0; i < payload.size(); i++) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
                EntityData record = payload.get(i);
                sortedRecords.add(record);
            }
        }
        
        if (unitOfWorkLastMessage) {
            ArrayList<EntityData> dataToSend = new ArrayList<EntityData>();
            
            sort();
            
            for (EntityData record : sortedRecords) {
                if (dataToSend.size() >= rowsPerMessage) {
                    sendMessage(dataToSend, messageTarget, false);
                    dataToSend = new ArrayList<EntityData>();
                }
                dataToSend.add(record);
            }
            
            sortedRecords.clear();
            
            if (dataToSend != null && dataToSend.size() > 0) {
                sendMessage(dataToSend, messageTarget, true);
            }
        }
    }

    private void applySettings() {
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);
        String sortAttribute = properties.get(SORT_ATTRIBUTE);
        if (sortAttribute == null) {
            throw new IllegalStateException("The sort attribute must be specified.");
        }
        Model inputModel = this.getComponent().getInputModel();
        String[] joinAttributeElements = sortAttribute.split("[.]");
        if (joinAttributeElements.length != 2) {
            throw new IllegalStateException(
                    "The sort attribute must be specified as 'entity.attribute'");
        }
        sortAttributeId = inputModel.getAttributeByName(joinAttributeElements[0],
                joinAttributeElements[1]).getId();
        if (sortAttributeId == null) {
            throw new IllegalStateException(
                    "Sort attribute must be a valid 'entity.attribute' in the input model.");
        }
    }

    private void sort() {
        Collections.sort(sortedRecords, new Comparator<EntityData>() {
            @Override
            public int compare(EntityData o1, EntityData o2) {
                Object obj1 = o1.get(sortAttributeId);
                Object obj2 = o2.get(sortAttributeId);
                if ((obj1 instanceof Comparable || obj1 == null)
                        && (obj2 instanceof Comparable || obj2 == null)) {
                    return ObjectUtils.compare((Comparable<?>) obj1, (Comparable<?>) obj2);
                } else {
                    String str1 = obj1 != null ? obj1.toString() : null;
                    String str2 = obj2 != null ? obj2.toString() : null;
                    return ObjectUtils.compare(str1, str2);
                }
            }
        });
    }

}
