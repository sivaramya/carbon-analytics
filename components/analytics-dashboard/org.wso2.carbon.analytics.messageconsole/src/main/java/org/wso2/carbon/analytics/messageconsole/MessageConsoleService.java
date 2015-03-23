package org.wso2.carbon.analytics.messageconsole;

/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.dataservice.AnalyticsDSUtils;
import org.wso2.carbon.analytics.dataservice.AnalyticsDataService;
import org.wso2.carbon.analytics.dataservice.commons.SearchResultEntry;
import org.wso2.carbon.analytics.datasource.commons.AnalyticsSchema;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.RecordGroup;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.analytics.messageconsole.beans.ColumnBean;
import org.wso2.carbon.analytics.messageconsole.beans.EntityBean;
import org.wso2.carbon.analytics.messageconsole.beans.RecordBean;
import org.wso2.carbon.analytics.messageconsole.beans.RecordResultBean;
import org.wso2.carbon.analytics.messageconsole.beans.TableBean;
import org.wso2.carbon.analytics.messageconsole.exception.MessageConsoleException;
import org.wso2.carbon.analytics.messageconsole.internal.ServiceHolder;
import org.wso2.carbon.context.CarbonContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageConsoleService {

    private static final Log logger = LogFactory.getLog(MessageConsoleService.class);
    private static final String LUCENE = "lucene";

    private AnalyticsDataService analyticsDataService;

    public MessageConsoleService() {
        this.analyticsDataService = ServiceHolder.getAnalyticsDataService();
    }

    public List<String> listTables() throws MessageConsoleException {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (logger.isDebugEnabled()) {
            logger.debug("Getting table list from data layer for tenant:" + tenantId);
        }
        try {
            return analyticsDataService.listTables(tenantId);
        } catch (Exception e) {
            logger.error("Unable to get table list from Analytics data layer for tenant: " + tenantId, e);
            throw new MessageConsoleException("Unable to get table list from Analytics data layer for tenant: " +
                                              tenantId, e);
        }
    }

    public RecordResultBean getRecords(String tableName, long timeFrom, long timeTo, int startIndex, int recordCount,
                                       String searchQuery)
            throws MessageConsoleException {

        if (logger.isDebugEnabled()) {
            logger.debug("Search Query: " + searchQuery);
            logger.debug("timeFrom: " + timeFrom);
            logger.debug("timeTo: " + timeTo);
            logger.debug("Start Index: " + startIndex);
            logger.debug("Page Size: " + recordCount);
        }

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        RecordResultBean recordResult = new RecordResultBean();
        RecordGroup[] results;
        int searchCount = 0;

        if (searchQuery != null && !searchQuery.isEmpty()) {
            try {
                List<SearchResultEntry> searchResults = analyticsDataService.search(tenantId, tableName, LUCENE,
                                                                                    searchQuery, startIndex, recordCount);
                List<String> ids = getRecordIds(searchResults);
                results = analyticsDataService.get(tenantId, tableName, 1, null, ids);
                searchCount = analyticsDataService.searchCount(tenantId, tableName, LUCENE, searchQuery);

                if (logger.isDebugEnabled()) {
                    logger.debug("Query satisfied result count: " + searchResults.size());
                }
            } catch (Exception e) {
                logger.error("Unable to get search indices from Analytics data layer for tenant: " + tenantId +
                             " and for table:" + tableName, e);
                throw new MessageConsoleException("Unable to get indices from Analytics data layer for tenant: " + tenantId +
                                                  " and for table:" + tableName, e);
            }
        } else {
            try {
                results = analyticsDataService.get(tenantId, tableName, 1, null, timeFrom, timeTo, startIndex, recordCount);
            } catch (Exception e) {
                logger.error("Unable to get records from Analytics data layer for tenant: " + tenantId +
                             " and for table:" + tableName, e);
                throw new MessageConsoleException("Unable to get records from Analytics data layer for tenant: " + tenantId +
                                                  " and for table:" + tableName, e);
            }
        }

        if (results != null) {
            List<RecordBean> recordBeanList = new ArrayList<>();
            List<Record> records;
            try {
                records = AnalyticsDSUtils.listRecords(analyticsDataService, results);
            } catch (Exception e) {
                logger.error("Unable to convert result to record for tenant: " + tenantId +
                             " and for table:" + tableName, e);
                throw new MessageConsoleException("Unable to convert result to record for tenant: " + tenantId +
                                                  " and for table:" + tableName, e);
            }
            if (records != null && !records.isEmpty()) {
                for (Record record : records) {
                    recordBeanList.add(createRecordBean(record));
                }
            }
            RecordBean[] recordBeans = new RecordBean[recordBeanList.size()];
            recordResult.setRecords(recordBeanList.toArray(recordBeans));
            searchCount = recordBeanList.size();
            recordResult.setTotalResultCount(searchCount);
        }

        return recordResult;
    }

    private RecordBean createRecordBean(Record record) {
        RecordBean recordBean = new RecordBean();
        recordBean.setRecordId(record.getId());
        recordBean.setTimestamp(record.getTimestamp());
        EntityBean[] entityBeans = new EntityBean[record.getValues().size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : record.getValues().entrySet()) {
            EntityBean entityBean = new EntityBean(entry.getKey(), String.valueOf(entry.getValue()));
            entityBeans[i++] = entityBean;
        }
        recordBean.setEntityBeans(entityBeans);

        return recordBean;
    }

    private List<String> getRecordIds(List<SearchResultEntry> searchResults) {
        List<String> ids = new ArrayList<String>(searchResults.size());
        for (SearchResultEntry searchResult : searchResults) {
            ids.add(searchResult.getId());
        }
        return ids;
    }

    public TableBean getTableInfo(String tableName) throws MessageConsoleException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        TableBean table = new TableBean();
        table.setName(tableName);
        try {
            AnalyticsSchema schema = analyticsDataService.getTableSchema(tenantId, tableName);
            ColumnBean[] columns = new ColumnBean[schema.getColumns().size()];

            Map<String, AnalyticsSchema.ColumnType> columnTypeMap = schema.getColumns();
            int i = 0;
            for (Map.Entry<String, AnalyticsSchema.ColumnType> stringColumnTypeEntry : columnTypeMap.entrySet()) {
                ColumnBean column = new ColumnBean();
                column.setName(stringColumnTypeEntry.getKey());
                column.setType(stringColumnTypeEntry.getValue().name());
                column.setPrimary(schema.getPrimaryKeys().contains(stringColumnTypeEntry.getKey()));
                columns[i++] = column;
            }
            table.setColumns(columns);
        } catch (Exception e) {
            logger.error("Unable to get schema information for table :" + tableName, e);
            throw new MessageConsoleException("Unable to get schema information for table :" + tableName, e);
        }

        return table;
    }

    public void deleteRecords(String table, String[] recordIds) throws MessageConsoleException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        String ids = Arrays.toString(recordIds);
        if (logger.isDebugEnabled()) {
            logger.debug(ids + " are going to delete from " + table + " in tenant:" + tenantId);
        }
        try {
            analyticsDataService.delete(tenantId, table, Arrays.asList(recordIds));
        } catch (Exception e) {
            logger.error("Unable to delete records" + ids + " from table :" + table, e);
            throw new MessageConsoleException("Unable to delete records" + ids + " from table :" + table, e);
        }
    }

    public RecordBean addRecord(String table, String[] columns, String[] values) throws MessageConsoleException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (logger.isDebugEnabled()) {
            logger.debug("New record {column: " + Arrays.toString(columns) + ", values: " + Arrays.toString(values) +
                         "} going to add to" + table);
        }

        RecordBean recordBean;
        try {
            Map<String, Object> objectMap = getRecordPropertyMap(table, columns, values, tenantId);

            Record record = new Record(tenantId, table, objectMap, System.currentTimeMillis());
            recordBean = createRecordBean(record);

            List<Record> records = new ArrayList<>(1);
            records.add(record);
            analyticsDataService.put(records);
        } catch (Exception e) {
            logger.error("Unable to add record {column: " + Arrays.toString(columns) + ", values: " + Arrays.toString(values) + " } to table :" + table, e);
            throw new MessageConsoleException("Unable to add record {column: " + Arrays.toString(columns) + ", values: " + Arrays.toString(values) + " } to table :" + table, e);
        }

        return recordBean;
    }

    public RecordBean updateRecord(String table, String recordId, String[] columns, String[] values, long timestamp)
            throws
            MessageConsoleException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (logger.isDebugEnabled()) {
            logger.debug("Record {id: " + recordId + ", column: " + Arrays.toString(columns) + ", values: " + Arrays.toString
                    (values) +
                         "} going to update to" + table);
        }

        RecordBean recordBean;
        try {
            Map<String, Object> objectMap = getRecordPropertyMap(table, columns, values, tenantId);

            Record record = new Record(recordId, tenantId, table, objectMap, timestamp);
            recordBean = createRecordBean(record);

            List<Record> records = new ArrayList<>(1);
            records.add(record);
            analyticsDataService.put(records);
        } catch (Exception e) {
            logger.error("Unable to update record {id: " + recordId + ", column: " + Arrays.toString(columns) + ", " +
                         "values: " + Arrays.toString(values) + " } to table :" + table, e);
            throw new MessageConsoleException("Unable to update record {id: " + recordId + ", column: " + Arrays
                    .toString(columns) + ", values: " + Arrays.toString(values) + " } to table :" + table, e);
        }

        return recordBean;
    }

    private Map<String, Object> getRecordPropertyMap(String table, String[] columns, String[] values, int tenantId)
            throws AnalyticsException {
        AnalyticsSchema schema = analyticsDataService.getTableSchema(tenantId, table);
        Map<String, AnalyticsSchema.ColumnType> columnsMetaInfo = schema.getColumns();

        Map<String, Object> objectMap = new HashMap<>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            String columnName = columns[i];
            String stringValue = values[i];
            if (columnName != null) {
                AnalyticsSchema.ColumnType columnType = columnsMetaInfo.get(columnName);
                Object value = stringValue;
                switch (columnType) {
                    case STRING:
                        break;
                    case INT:
                        value = Integer.valueOf(stringValue);
                        break;
                    case LONG:
                        value = Long.valueOf(stringValue);
                        break;
                    case BOOLEAN:
                        value = Boolean.valueOf(stringValue);
                        break;
                    case FLOAT:
                        value = Float.valueOf(stringValue);
                        break;
                    case DOUBLE:
                        value = Double.valueOf(stringValue);
                        break;
                }
                objectMap.put(columnName, value);
            }
        }
        return objectMap;
    }

    public EntityBean[] getArbitraryList(String table, String recordId) throws MessageConsoleException {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        List<EntityBean> entityBeansList = new ArrayList<>();
        List<String> ids = new ArrayList<>(1);
        ids.add(recordId);
        try {
            RecordGroup[] results = analyticsDataService.get(tenantId, table, 1, null, ids);
            List<Record> records = AnalyticsDSUtils.listRecords(analyticsDataService, results);
            AnalyticsSchema schema = analyticsDataService.getTableSchema(tenantId, table);

            if (records != null && !records.isEmpty()) {
                Map<String, AnalyticsSchema.ColumnType> schemaColumns = schema.getColumns();
                Record record = records.get(0);
                Map<String, Object> recordValues = record.getValues();
                for (Map.Entry<String, Object> objectEntry : recordValues.entrySet()) {
                    if (!schemaColumns.containsKey(objectEntry.getKey())) {
                        EntityBean entityBean;
                        if (objectEntry.getValue() != null) {
                            entityBean = new EntityBean(objectEntry.getKey(), String.valueOf(objectEntry.getValue()), objectEntry
                                    .getValue().getClass().getSimpleName());

                        } else {
                            entityBean = new EntityBean(objectEntry.getKey(), "NULL", "String");
                        }
                        entityBeansList.add(entityBean);
                    }
                }
            }
        } catch (AnalyticsException e) {
            logger.error("Unable to get arbitrary fields for id [" + recordId + "] from table :" + table, e);
            throw new MessageConsoleException("Unable to get arbitrary fields for id [" + recordId + "] from table :" + table, e);
        }

        EntityBean[] entityBeans = new EntityBean[entityBeansList.size()];
        return entityBeansList.toArray(entityBeans);
    }

    public void deleteArbitraryField(String table, String recordId, String fieldName) throws MessageConsoleException {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        List<String> ids = new ArrayList<>(1);
        ids.add(recordId);
        try {
            RecordGroup[] results = analyticsDataService.get(tenantId, table, 1, null, ids);
            List<Record> records = AnalyticsDSUtils.listRecords(analyticsDataService, results);
            if (records != null && !records.isEmpty()) {
                Record record = records.get(0);
                Map<String, Object> recordValues = record.getValues();
                recordValues.remove(fieldName);
                Record editedRecord = new Record(recordId, tenantId, table, recordValues, record.getTimestamp());
                records.clear();
                records.add(editedRecord);
                analyticsDataService.put(records);
            }
        } catch (AnalyticsException e) {
            logger.error("Unable to delete arbitrary field[" + fieldName + "] for id [" + recordId + "] from table :" + table, e);
            throw new MessageConsoleException("Unable to arbitrary arbitrary field[" + fieldName + "] for id [" + recordId + "] from table :" + table, e);
        }
    }

    public void putArbitraryField(String table, String recordId, String fieldName, String value, String type)
            throws MessageConsoleException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        List<String> ids = new ArrayList<>(1);
        ids.add(recordId);
        try {
            RecordGroup[] results = analyticsDataService.get(tenantId, table, 1, null, ids);
            List<Record> records = AnalyticsDSUtils.listRecords(analyticsDataService, results);
            if (records != null && !records.isEmpty()) {
                Record record = records.get(0);
                Map<String, Object> recordValues = record.getValues();
                recordValues.remove(fieldName);
                Object convertedValue;
                switch (type) {
                    case "String": {
                        convertedValue = String.valueOf(value);
                        break;
                    }
                    case "Integer": {
                        convertedValue = Integer.valueOf(value);
                        break;
                    }
                    case "Boolean": {
                        convertedValue = Boolean.valueOf(value);
                        break;
                    }
                    case "Float": {
                        convertedValue = Float.valueOf(value);
                        break;
                    }
                    case "Double": {
                        convertedValue = Double.valueOf(value);
                        break;
                    }
                    default: {
                        convertedValue = value;
                    }
                }

                recordValues.put(fieldName, convertedValue);

                records.clear();
                Record editedRecord = new Record(recordId, tenantId, table, recordValues, record.getTimestamp());
                records.add(editedRecord);
                analyticsDataService.put(records);
            }
        } catch (AnalyticsException e) {
            logger.error("Unable to update arbitrary field[" + fieldName + "] for id [" + recordId + "] from table :" + table, e);
            throw new MessageConsoleException("Unable to update arbitrary field[" + fieldName + "] for id [" + recordId + "] from table :" + table, e);
        }
    }
}