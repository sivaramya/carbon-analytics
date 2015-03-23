var tableLoaded = false;
var timestamp;
var arbitraryColumnName;
function createMainJTable(fields) {
    $('#AnalyticsTableContainer').jtable({
                                             title: $("#tableSelect").val(),
                                             paging: true,
                                             pageSize: 25,
                                             selecting: true,
                                             multiselect: true,
                                             selectingCheckboxes: true,
                                             actions: {
                                                 // For Details: http://jtable.org/Demo/FunctionsAsActions
                                                 listAction: function (postData, jtParams) {
                                                     return listActionMethod(jtParams);
                                                 },
                                                 createAction: function (postData) {
                                                     return createActionMethod(postData);
                                                 },
                                                 updateAction: function (postData) {
                                                     return updateActionMethod(postData);
                                                 },
                                                 deleteAction: function (postData) {
                                                     return deleteActionMethod(postData);
                                                 }
                                             },
                                             formCreated: function (event, data) {
                                                 timestamp = data.record.bam_rec_timestamp;
                                             },
                                             fields: fields

                                         });
    $('#AnalyticsTableContainer').jtable('load');
    $("#DeleteAllButton").show();
    $('#DeleteAllButton').button().click(function () {
        var $selectedRows = $('#AnalyticsTableContainer').jtable('selectedRows');
        $('#AnalyticsTableContainer').jtable('deleteRows', $selectedRows);
    });
    tableLoaded = true;
}

function getArbitraryFields(rowData) {
    var $img =
            $('<img src="/carbon/messageconsole/themes/metro/list_metro.png" title="Show Arbitrary Fields"/>');
    $img.click(function () {
        $('#AnalyticsTableContainer').jtable('openChildTable',
                                             $img.closest('tr'), //Parent row
                                             {
                                                 title: 'Arbitrary Fields',
                                                 selecting: true,
                                                 messages: {
                                                     addNewRecord: 'Add new arbitrary field'
                                                 },
                                                 actions: {
                                                     // For Details: http://jtable.org/Demo/FunctionsAsActions
                                                     listAction: function (postData, jtParams) {
                                                         var postData = {};
                                                         postData['tableName'] = $("#tableSelect").val();
                                                         postData['bam_unique_rec_id'] = rowData.record.bam_unique_rec_id;
                                                         return arbitraryFieldListActionMethod(postData, jtParams);
                                                     },
                                                     createAction: function (postData) {
                                                         return arbitraryFieldCreateActionMethod(postData);
                                                     },
                                                     updateAction: function (postData) {
                                                         return arbitraryFieldUpdateActionMethod(postData);
                                                     },
                                                     deleteAction: function (postData) {
                                                         postData['tableName'] = $("#tableSelect").val();
                                                         postData['bam_unique_rec_id'] = rowData.record.bam_unique_rec_id;
                                                         return arbitraryFieldDeleteActionMethod(postData);
                                                     }
                                                 },
                                                 deleteConfirmation: function (data) {
                                                     arbitraryColumnName = data.record.Name;
                                                 },
                                                 rowsRemoved: function (event, data) {
                                                     arbitraryColumnName ="";
                                                 },
                                                 fields: {
                                                     bam_unique_rec_id: {
                                                         type: 'hidden',
                                                         key: true,
                                                         list: false,
                                                         defaultValue: rowData.record.bam_unique_rec_id
                                                     },
                                                     Name: {
                                                         title: 'Name'
                                                     },
                                                     Value: {
                                                         title: 'Value'
                                                     },
                                                     Type: {
                                                         title: 'Type',
                                                         options: ["String", "Integer", "LOng", "Boolean", "Float", "Double"]
                                                     }
                                                 }
                                             }, function (data) { //opened handler
                    data.childTable.jtable('load');
                }
        );
    });
    return $img;
}

function arbitraryFieldListActionMethod(postData, jtParams) {
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeListArbitraryRecord,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               }
        );
    });
}

function arbitraryFieldCreateActionMethod(postData) {
    var recordId;
    var $selectedRows = $('#AnalyticsTableContainer').jtable('selectedRows');
    if ($selectedRows.length > 0) {
        $selectedRows.each(function () {
            var record = $(this).data('record');
            recordId = record.bam_unique_rec_id;
        });
    }
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeCreateArbitraryRecord + "&tableName=" + $("#tableSelect").val() + "&bam_unique_rec_id=" + recordId,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               }
        );
    });
}

function arbitraryFieldUpdateActionMethod(postData) {
    var recordId;
    var $selectedRows = $('#AnalyticsTableContainer').jtable('selectedRows');
    if ($selectedRows.length > 0) {
        $selectedRows.each(function () {
            var record = $(this).data('record');
            recordId = record.bam_unique_rec_id;
        });
    }
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeUpdateArbitraryRecord + "&tableName=" + $("#tableSelect").val() + "&bam_unique_rec_id=" + recordId,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               }
        );
    });
}

function arbitraryFieldDeleteActionMethod(postData) {
    postData["Name"] = arbitraryColumnName;
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeDeleteArbitraryRecord,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               }
        );
    });
}

function createJTable() {
    var table = $("#tableSelect").val();
    if (table != '-1') {
        $.getJSON("/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=" + typeTableInfo + "&tableName=" + table,
                  function (data, status) {
                      var fields = {
                          ArbitraryFields: {
                              title: '',
                              width: '2%',
                              sorting: false,
                              edit: false,
                              create: false,
                              display: function (rowData) {
                                  return getArbitraryFields(rowData);
                              }
                          }
                      };
                      $.each(data.columns, function (key, val) {
                          fields[val.name] = {
                              title: val.name,
                              list: val.display,
                              key: val.key
                          };
                          if (val.type == 'STRING') {
                              fields[val.name].type = 'textarea';
                          }
                          if (val.name == 'bam_unique_rec_id' || val.name == 'bam_rec_timestamp') {
                              fields[val.name].edit = false;
                              fields[val.name].create = false;
                          }
                      });
                      if (data) {
                          if (tableLoaded == true) {
                              $('#AnalyticsTableContainer').jtable('destroy');
                          }
                          createMainJTable(fields);
                      }
                  }
        );
    }
}

function listActionMethod(jtParams) {
    var postData = {};
    postData["jtStartIndex"] = jtParams.jtStartIndex;
    postData["jtPageSize"] = jtParams.jtPageSize;
    postData["tableName"] = $("#tableSelect").val();
    postData["timeFrom"] = $("#timeFrom").val();
    postData["timeTo"] = $("#timeTo").val();
    postData["query"] = $("#query").val();
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeListRecord,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               }
        );
    });
}

function createActionMethod(postData) {
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeCreateRecord + '&tableName=' + $("#tableSelect").val(),
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               });
    });
}

function updateActionMethod(postData) {
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?' +
                        'type=' + typeUpdateRecord + '&tableName=' + $("#tableSelect").val() + '&bam_rec_timestamp=' + timestamp,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               });
    });
}

function deleteRecords(postData) {
    postData["tableName"] = $("#tableSelect").val();
    return $.Deferred(function ($dfd) {
        $.ajax({
                   url: '/carbon/messageconsole/messageconsole_ajaxprocessor.jsp?type=' + typeDeleteRecord,
                   type: 'POST',
                   dataType: 'json',
                   data: postData,
                   success: function (data) {
                       $dfd.resolve(data);
                   },
                   error: function () {
                       $dfd.reject();
                   }
               }
        );
    });
}

function deleteActionMethod(postData) {
    return deleteRecords(postData);
}