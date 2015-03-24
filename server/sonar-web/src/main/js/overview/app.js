requirejs([
  'overview/layout',
  'overview/models/state',
  'overview/views/gate-view',
  'overview/views/size-view',
  'overview/views/issues-view',
  'overview/views/coverage-view',
  'overview/views/duplications-view'
], function (Layout,
             State,
             GateView,
             SizeView,
             IssuesView,
             CoverageView,
             DuplicationsView) {

  var App = new Marionette.Application();

  App.addInitializer(function () {
    this.state = new State(window.overviewConf);
    this.layout = new Layout({
      el: '.overview',
      model: this.state
    }).render();
    this.layout.gateRegion.show(new GateView({ model: this.state }));
    this.layout.sizeRegion.show(new SizeView({ model: this.state }));
    this.layout.issuesRegion.show(new IssuesView({ model: this.state }));
    this.layout.coverageRegion.show(new CoverageView({ model: this.state }));
    this.layout.duplicationsRegion.show(new DuplicationsView({ model: this.state }));
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
