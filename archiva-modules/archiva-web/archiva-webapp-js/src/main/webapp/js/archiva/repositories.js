/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
$(function() {

  ManagedRepository=function(id,name,layout,indexDirectory,location,snapshots,releases,blockRedeployments,cronExpression,
                             scanned,daysOlder,retentionCount,deleteReleasedSnapshots,stageRepoNeeded){


    //private String id;
    this.id=ko.observable(id);

    //private String name;
    this.name=ko.observable(name);

    //private String layout = "default";
    this.layout=ko.observable(layout);

    //private String indexDirectory;
    this.indexDirectory=ko.observable(indexDirectory);

    //private String location;
    this.location=ko.observable(location);

    //private boolean snapshots = false;
    this.snapshots=ko.observable(snapshots);

    //private boolean releases = true;
    this.releases=ko.observable(releases);

    //private boolean blockRedeployments = false;
    this.blockRedeployments=ko.observable(blockRedeployments);

    //private String cronExpression = "0 0 * * * ?";
    this.cronExpression=ko.observable(cronExpression);

    //private ManagedRepository stagingRepository;

    //private boolean scanned = false;
    this.scanned=ko.observable(scanned);

    //private int daysOlder = 100;
    this.daysOlder=ko.observable(daysOlder);

    //private int retentionCount = 2;
    this.retentionCount=ko.observable(retentionCount);

    //private boolean deleteReleasedSnapshots;
    this.deleteReleasedSnapshots=ko.observable(deleteReleasedSnapshots);

    //private boolean stageRepoNeeded;
    this.stageRepoNeeded=ko.observable(stageRepoNeeded);
  }

  ManagedRepositoriesViewModel=function(){
    this.managedRepositories=ko.observableArray(new Array());

    this.gridViewModel = null;

    editManagedRepository=function(managedRepository){
      $.log(managedRepository.name());
    }

  }


  displayRepositoriesGrid=function(){
    clearUserMessages();
    $("#main-content").html(mediumSpinnerImg());
    $("#main-content").html($("#repositoriesMain").tmpl());
    $("#repositories-tabs").tabs();

    $("#main-content #managed-repositories-content").append(mediumSpinnerImg());
    $("#main-content #remote-repositories-content").append(mediumSpinnerImg());



    $.ajax("restServices/archivaServices/managedRepositoriesService/getManagedRepositories", {
        type: "GET",
        dataType: 'json',
        success: function(data) {

          var managedRepositoriesViewModel = new ManagedRepositoriesViewModel();
          managedRepositoriesViewModel.managedRepositories(mapManagedRepositories(data));
          managedRepositoriesViewModel.gridViewModel = new ko.simpleGrid.viewModel({
            data: managedRepositoriesViewModel.managedRepositories,
            columns: [
              {
                headerText: $.i18n.prop('identifier'),
                rowText: "id"
              },
              {
                headerText: $.i18n.prop('name'),
                rowText: "name"
              },
              {
                headerText: $.i18n.prop('type'),
                rowText: "layout"
              }
            ],
            pageSize: 10
          });
          ko.applyBindings(managedRepositoriesViewModel,$("#main-content #managed-repositories-table").get(0));
          $("#main-content #managed-repositories-pills").pills();
          $("#managed-repositories-view").addClass("active");
          removeMediumSpinnerImg("#main-content #managed-repositories-content");
          $.log("ok display managedRepositories");
        }
      }
    );

  }

  mapManagedRepositories=function(data){
    var mappedManagedRepositories = $.map(data.managedRepository, function(item) {
      return mapManagedRepository(item);
    });
    return mappedManagedRepositories;
  }
  mapManagedRepository=function(data){

    return new ManagedRepository(data.id,data.name,data.layout,data.indexDirectory,data.location,data.snapshots,data.releases,
                                 data.blockRedeployments,data.cronExpression,
                                 data.scanned,data.daysOlder,data.retentionCount,data.deleteReleasedSnapshots,data.stageRepoNeeded);
  }

});