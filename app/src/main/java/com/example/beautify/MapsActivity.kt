package com.example.beautify

//import com.google.maps.example.R

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.Color.argb
import android.graphics.Color.rgb
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.internal.Constants
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonArray
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject


class MapsActivity : AppCompatActivity(), OnMyLocationButtonClickListener,
    OnMyLocationClickListener, OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var beau = argb(75,46,148,38)
    lateinit var geofencingClient: GeofencingClient
    lateinit var currPos: Location
    private var payable :Boolean = false
    private val circleList = mutableListOf<Circle>()
     val nameList = mutableListOf<String>()
    private val latList = mutableListOf<Double>()
    private val logList = mutableListOf<Double>()
    private var nearCirc: Circle? = null
    private val range : Double = 300.0

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        //run("https://tashnash.github.io/locations.json")

        //mimic()
        val fab: View = findViewById(R.id.camButton)
        fab.setOnClickListener {
            val intent = Intent(this, CameraIn::class.java).apply {

            }
            startActivity(intent)
        }
        val fab2: View = findViewById(R.id.shop_button)
        fab2.setOnClickListener {
            val intent = Intent(this, ShopActivity::class.java).apply {

            }
            startActivity(intent)
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)


        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                camReBound(LatLng(location!!.latitude, location.longitude))
            }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return
                for (location in p0.locations) {
                    camReBound(LatLng(location!!.latitude, location.longitude))
                    currPos = location

                    nearCirc = circleList[nearestCircle()]

                    payable = inRange(LatLng(location.latitude, location.longitude), nearCirc!!.center)
                    Log.d("pay","B $payable")
                }
            }

        }
        startLocationUpdates()
    }




    private var ob : JSONArray = JSONArray()
    private var back =""
    private fun run(url: String) {

        val queue = Volley.newRequestQueue(this)
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                ob = response
                Log.d("msg7", "Handling ${ob.toString()}")
                back.plus(response.toString())
                Log.d("msg3", response.toString())

                for (i in 0 until ob.length()) {
                    Log.d("msg5","Handling ${ob.toString()}")

                    val loc = ob.getJSONObject(i)
                    nameList.add("${loc.get("locationName")}")
                    Log.d("msg5","Pushing ${nameList[i]}")
                    latList.add(("${loc.get("xCoord")}").toDouble())
                    Log.d("msg5","Pushing ${latList[i]}")
                    logList.add(("${loc.get("yCoord")}").toDouble())
                    Log.d("msg5","Pushing ${logList[i]}")
                    if(i ==ob.length()-1) {
                        mimic()
                        processMimic()
                    }
                }
            },
            { error ->
                Log.d("msg4", "yesss$error")
            }
        )
        jsonArrayRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(jsonArrayRequest)

    }


//    var tempString:String
//    fun run(url: String) {
//        val request = Request.Builder()
//            .url(url)
//            .build()
//
//        val temp = "hi";
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {}
//            override fun onResponse(call: Call, response: Response) : String =
//                println(response.body()?.string())
//                return(response.body()?.string())
//        })
//        Log.d("plsWork", )
//    }

    private val structureList = mutableListOf<Structure>()
    private fun mimic() {
        Log.d("msg10","Holding ${nameList}")
        Log.d("msg10","Holding ${latList}")
        Log.d("msg10","Holding ${logList}")
        for(i in nameList.indices) {
            structureList.add(Structure(nameList[i],latList[i],logList[i]))
        }
        //structureList.add(Structure("fulmer dumpster",33.778525,-84.403702))
        //structureList.add(Structure("willage",33.779218,-84.405114))
        //structureList.add(Structure("brittain",33.772293,-84.391277))
    }
    private fun processMimic(){
        Log.d("msg10","Sending ${structureList}")
        for (circle in structureList) {
            addArea(LatLng(circle.xCoord,circle.yCoord),circle.locationName)
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isRotateGesturesEnabled = false;
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        mMap.setMinZoomPreference(14.0f)
        mMap.setMaxZoomPreference(16.0f)
        // Add a marker in Sydney and move the camera
        addMarker("GaTech",33.775778305161886, -84.39633864568813)
        addArea(LatLng(33.775778305161886, -84.39633864568813),"GaTech")

        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        //processMimic()
        run("https://tashnash.github.io/locations.json")

    }
    private val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }


    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG)
            .show()
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT)
            .show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    private fun addMarker(name:String, lat:Double, log:Double) {
        val ref = LatLng(lat, log)
        mMap.addMarker(MarkerOptions()
            .position(ref)
            .title("Marker in $name")
            .alpha(0.75F)
            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_green_leaf_vegan_icon_by_vexels))
            .icon(BitmapDescriptorFactory.defaultMarker(110F))
            //.flat(true)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ref))
    }
    private fun camReBound(latLng : LatLng) {
        val builder = LatLngBounds.builder()
        val bounds = builder.include(latLng).build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,300,300, 30))
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private val locationPermissionRequest = registerForActivityResult(
        RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            } else -> {
            // No location access granted.
        }
        }
    }

    private var requestingLocationUpdates :Boolean = true

    override fun onResume() {
        super.onResume()

        if (requestingLocationUpdates) startLocationUpdates()
    }



    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



    private fun addArea(latLng: LatLng, name:String) {

        val circle: Circle = mMap.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(range)
                .strokeColor(Color.BLACK)
                .fillColor(beau)
                .clickable(true)
        )
        circle.tag = name
        circleList.add(circle)
    /**
        geofenceList.add(Geofence.Builder()
            .setRequestId(name)
            .setCircularRegion(
                latLng.latitude,
                latLng.longitude,
                1000.0F
            )
            .setExpirationDuration(2000000)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build())*/
    }

    private fun checkDistTo(latLng1: LatLng,latLng: LatLng) : Float{
        val arr2 = FloatArray(1)
        Location.distanceBetween(latLng1.latitude,latLng1.longitude,latLng.latitude,latLng.longitude,arr2)
        return arr2[0]
    }

    private fun inRange(latLng1: LatLng,latLng: LatLng) : Boolean{
        val arr2 = FloatArray(1)
        Location.distanceBetween(latLng1.latitude,latLng1.longitude,latLng.latitude,latLng.longitude,arr2)
        return arr2[0] <= range
    }

    private fun nearestCircle():Int {
        var mindex = 0
        var mistance = 1000000
        for ((i, circle) in circleList.withIndex()) {

            var distance = checkDistTo(LatLng(currPos.latitude,currPos.longitude),circle.center)
            if(distance < mistance) {
                mindex =  i
            }
        }

        return mindex

    }


}





