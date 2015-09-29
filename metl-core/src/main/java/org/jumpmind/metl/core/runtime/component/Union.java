package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class Union extends AbstractComponentRuntime {

    public static final String TYPE = "Union";

    List<FlowStepLink> flowStepLinks;
    
    Map<String, List<Message>> messagesByFlowStep;
    
    @Override
    protected void start() {
        flowStepLinks = getFlow().findFlowStepLinksWithTarget(getFlowStepId());
        messagesByFlowStep = new HashMap<String, List<Message>>();
        for (FlowStepLink flowStepLink : flowStepLinks) {
            messagesByFlowStep.put(flowStepLink.getSourceStepId(), new ArrayList<Message>());
        }
    }
    
    protected void initMessagesByFlowStep() {
        
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        
        String fromStepId = inputMessage.getHeader().getOriginatingStepId();
        List<Message> messages = messagesByFlowStep.get(fromStepId);
        if (messages != null) {
            messages.add(inputMessage);
        }

        
        if (unitOfWorkLastMessage) {
            ArrayList<EntityData> rowData = new ArrayList<EntityData>();
            for (List<Message> unhandledMessages : messagesByFlowStep.values()) {
                Message message = unhandledMessages.remove(0);
                ArrayList<EntityData> inputRowData = message.getPayload();
                rowData.addAll(inputRowData);
                getComponentStatistics().incrementNumberEntitiesProcessed(inputRowData.size());
            }
            sendMessage(rowData, messageTarget, unitOfWorkLastMessage);
        }
    }

}
