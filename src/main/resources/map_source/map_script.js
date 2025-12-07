// --- Set Map Bounds with extra north and west space ---
var southWest = L.latLng(10.99, 122.75);
var northEast = L.latLng(11.10, 122.84);
var bounds = L.latLngBounds(southWest, northEast);

// --- Initialize Map with constrained bounds ---
var map = L.map('map', {
    center: [11.064, 122.772],
    zoom: 12,
    maxZoom: 19,
    minZoom: 10,
    maxBounds: bounds,
    maxBoundsViscosity: 1.0 // fully constrain map
});

// --- Base Layers ---
var osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
});

var esriSat = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles © Esri'
});

var esriLabels = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Labels © Esri'
});

var hybrid = L.layerGroup([esriSat, esriLabels]);

// Additional Base Layers
var topo = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
    maxZoom: 17,
    attribution: '© OpenTopoMap'
});

var dark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; <a href="https://carto.com/">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 19
});

// --- Default layer: Hybrid ---
hybrid.addTo(map);

// --- Layer Control ---
var baseMaps = {
    "Street Map": osm,
    "Hybrid Map": hybrid,
    "Topographic": topo,
    "Dark Mode": dark
};
L.control.layers(baseMaps).addTo(map);

// --- Barangays Data ---
var barangays = [
    {name: "Managopaya", coords: [11.087870692060337, 122.76484940764112]},
    {name: "Libertad", coords: [11.02472298293963, 122.79815587477945]},
    {name: "Poblacion", coords: [11.000809, 122.818911]},
    {name: "Carmelo", coords: [11.00836418907593, 122.8165942591084]},
    {name: "Belen", coords: [11.005436161769929, 122.79974389645737]},
    {name: "Bariga", coords: [11.056641004882964, 122.7836045591135]},
    {name: "Talokgangan", coords: [11.006401518716034, 122.8339658646945]},
    {name: "San Salvador", coords: [10.998237, 122.834187]},
    {name: "Bularan", coords: [11.002859341803942, 122.82372069420383]},
    {name: "Zunasor", coords: [11.000462577852637, 122.81476616417326]},
    {name: "Alacaygan", coords: [10.998561854906102, 122.80856652684305]},
    {name: "Fuentes", coords: [10.995655302761172, 122.78693386773779]},
    {name: "Dugwakan", coords: [11.011945099445292, 122.78907211631092]},
    {name: "Merced", coords: [11.027173605223176, 122.82174853595093]},
    {name: "Magdalo", coords: [11.044573047520263, 122.8005317093123]},
    {name: "Bobon", coords: [11.066425825647809, 122.78997530312927]},
    {name: "Juanico", coords: [11.081232705492647, 122.78346959558888]},
    {name: "Delapaz", coords: [11.070695649883818, 122.76894591725629]}
];

// --- Circle Center & Radius ---
var latSum = 0, lngSum = 0;
barangays.forEach(b => {
    latSum += b.coords[0];
    lngSum += b.coords[1];
});
var centerLat = latSum / barangays.length;
var centerLng = lngSum / barangays.length;

var shift = 0.01;
var circleCenter = [centerLat + shift, centerLng];

var maxDistance = 0;
barangays.forEach(b => {
    var distance = map.distance(circleCenter, b.coords);
    if (distance > maxDistance)
        maxDistance = distance;
});

// --- Draw Blue Circle (without popup) ---
var barangayCircle = L.circle(circleCenter, {
    radius: maxDistance + 20,
    color: 'blue',
    weight: 2,
    fillColor: 'rgba(0,0,255,0.2)',
    fillOpacity: 0.4
}).addTo(map);

barangays.forEach(b => {
    if (barangayCircle.getLatLng().distanceTo(b.coords) <= barangayCircle.getRadius()) {
        L.marker(b.coords).addTo(map).bindPopup(b.name);
    }
});

map.fitBounds(barangayCircle.getBounds());

map.on('click', function (e) {
    var lat = e.latlng.lat.toFixed(6);
    var lng = e.latlng.lng.toFixed(6);
    L.popup()
        .setLatLng(e.latlng)
        .setContent("Latitude: " + lat + "<br>Longitude: " + lng)
        .openOn(map);
});