/*
 ~ Copyright (c) 2015  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~      http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 */

var httpUrl = location.protocol + "//" + location.host;
var CONTEXT = "bpmn-explorer";


$(document).ready(function () {

    function getFileType() {
        var fileName = document.getElementById('files').value;
        if (fileName !== null) {
            var extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            return extension;
        }
    }

    // Send attachment info the server
    $('#attachForm').ajaxForm({
        beforeSubmit: function (arr, formData, options) {
            for (var i = 0; i < arr.length; i++) {
                if (!arr[i].value) {
                    $('#submit-attachment').popover({
                        content: "Please enter all values",
                        placement: "right"
                    });
                    $('#submit-attachment').popover('show');

                    return false;
                }
            }

            setTimeout(function () {
                $('.btn-primary').popover('hide')
            }, 1000);
            var fileType = getFileType();
            if (fileType) {
                arr.push({name: 'type', value: fileType})
            }
        },

        success: function (res) {
            var taskUrl = res.taskUrl;
            var taskId = taskUrl.substr(taskUrl.lastIndexOf('/') + 1);
            window.location = httpUrl + "/" + CONTEXT + "/task?id=" + taskId;

        },
        error: function (res) {
            document.getElementById("error_content").style.visibility = 'visible';
        }
    });
});

function displayAttachmentData(id) {
    window.location = httpUrl + "/" + CONTEXT + "/task?id=" + id;
}
function completeTask(data, id) {
    var url = "/" + CONTEXT + "/send?req=/bpmn/runtime/tasks/" + id;
    var variables = [];
    for (var i = 0; i < data.length; i++) {
        variables.push({
            "name": data[i].name,
            "value": data[i].value
        });
    }
    var body = {
        "action": "complete",
        "variables": variables
    };

    $.ajax({
        type: 'POST',
        contentType: "application/json",
        url: httpUrl + url,
        data: JSON.stringify(body),
        success: function (data) {
            window.location = httpUrl + "/" + CONTEXT + "/myTasks";
        }
    });
}

function reassign(username, id) {
    var url = "/" + CONTEXT + "/send?req=/bpmn/runtime/tasks/" + id;
    var body = {
        "assignee": username
    };

    $.ajax({
        type: 'PUT',
        contentType: "application/json",
        url: httpUrl + url,
        data: JSON.stringify(body),
        success: function (data) {
            window.location = httpUrl + "/" + CONTEXT + "/myTasks";
        }
    });
}

function claim(username, id) {
    var url = "/" + CONTEXT + "/send?req=/bpmn/runtime/tasks/" + id;
    var body = {
        "assignee": username
    };

    $.ajax({
        type: 'PUT',
        contentType: "application/json",
        url: httpUrl + url,
        data: JSON.stringify(body),
        success: function (data) {
            window.location = httpUrl + "/" + CONTEXT + "/task?id=" + id;
        }
    });
}


function transfer(username, id) {
    var url = "/" + CONTEXT + "/send?req=/bpmn/runtime/tasks/" + id;
    var body = {
        "owner": username
    };

    $.ajax({
        type: 'PUT',
        contentType: "application/json",
        url: httpUrl + url,
        data: JSON.stringify(body),
        success: function (data) {
            window.location = httpUrl + "/" + CONTEXT + "/myTasks";
        }
    });
}


function startProcess(processDefId) {
    var url = "/" + CONTEXT + "/send?req=/bpmn/runtime/process-instances";
    var body = {
        "processDefinitionId": processDefId
    };

    $.ajax({
        type: 'POST',
        contentType: "application/json",
        url: httpUrl + url,
        data: JSON.stringify(body),
        success: function (data) {
            window.location = httpUrl + "/" + CONTEXT + "/process?startProcess=" + processDefId;
        },
        error: function (xhr, status, error) {
            window.location = httpUrl + "/" + CONTEXT + "/process?errorProcess=" + processDefId;
        }
    });
}

function startProcessWithData(data, id) {
    var url = "/" + CONTEXT + "/send?req=/bpmn/runtime/process-instances";
    var variables = [];
    for (var i = 0; i < data.length; i++) {
        variables.push({
            "name": data[i].name,
            "value": data[i].value
        });
    }
    var body = {
        "processDefinitionId": id,
        "variables": variables
    };
    $.ajax({
        type: 'POST',
        contentType: "application/json",
        url: httpUrl + url,
        data: JSON.stringify(body),
        success: function (data) {
            window.location = httpUrl + "/" + CONTEXT + "/process?startProcess=" + id;
        },
        error: function (xhr, status, error) {
            window.location = httpUrl + "/" + CONTEXT + "/process?errorProcess=" + id;
        }

    });
}
//Get the Task Durations Of Completed Processes
function selectProcessForChart() {
    var x = document.getElementById("selectProcess").value;
    var url = httpUrl + "/" + CONTEXT + "/stats?update=true&id=" + x;

    $.ajax({
        type: 'GET',
        contentType: "application/json",
        url: url,
        success: function (data) {

            var array = eval('(' + data + ')');
            google.load("visualization", "1", {packages: ["corechart"]});
            google.setOnLoadCallback(drawChart(array));

            function drawChart(data) {
                var dataArr = [['Task Key', 'Avg Duration']];
                for (var i = 0; i < data.length; i++) {
                    dataArr.push([data[i][0], data[i][1]]);

                }

                var data = google.visualization.arrayToDataTable(dataArr);

                var options = {
                    title: x,
                    pieHole: 0.6,
                    pieSliceTextStyle: {
                        color: 'black'
                    }
                };
                var chart = new google.visualization.PieChart(document.getElementById('taskDurationChart'));
                chart.draw(data, options);
            }
        }
    });
}