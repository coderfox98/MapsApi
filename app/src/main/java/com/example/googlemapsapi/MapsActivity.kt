package com.example.googlemapsapi

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3
    }


    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var lastLocation : Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                lastLocation = p0!!.lastLocation

                placeMarker(LatLng(lastLocation.latitude,lastLocation.longitude))
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()
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

    override fun onMarkerClick(p0: Marker?) = false

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
//
//
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)

//
//
//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(28.704060, 77.102493)
//
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,12.0f))
//
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        checkPermission()

        mMap.isMyLocationEnabled = true

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        fusedLocationClient.lastLocation.addOnSuccessListener(this){
            location ->
            if(location != null) {
                lastLocation = location
                val currentLatLong = LatLng(location.latitude,location.longitude)

                placeMarker(currentLatLong)

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,10.0f))
            }
        }
    }

     fun fabMethod(view: View) {
        loadPlacePicker()
    }

    private fun loadPlacePicker() {
        val builder = PlacePicker.IntentBuilder()

        try
        {
            startActivityForResult(builder.build(this@MapsActivity), PLACE_PICKER_REQUEST)
        }catch (e: GooglePlayServicesRepairableException) {
            Log.e("MapsActivity",e.toString())
        }catch (e : GooglePlayServicesNotAvailableException) {
            Log.e("MapsActivity",e.toString())
        }
    }

    private fun startLocationUpdates() {
        checkPermission()

        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)

    }

    private fun createLocationRequest(){

        locationRequest = LocationRequest()

        locationRequest.interval = 10000

        locationRequest.fastestInterval = 5000

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)

        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }

        task.addOnFailureListener{ e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
                } catch (e : IntentSender.SendIntentException){
                    Log.e("MapsActivity",e.toString())
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlacePicker.getPlace(this,data)
                var addressText = place.name.toString()
                addressText += "\n" + place.name.toString()

                placeMarker(place.latLng)
            }
        }

        if (resultCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK){
                locationUpdateState = true
                startLocationUpdates()
            }
        }

    }

    override fun onPause() {
        super.onPause()

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }

    }

    private fun getAddress(latLong: LatLng) : String {

        val geoCoder = Geocoder(this)
        val addresses : List<Address>?
        val address : Address
        var addressText = ""

        try {
            addresses = geoCoder.getFromLocation(latLong.latitude,latLong.longitude,1)
            if (addresses != null && !addresses.isEmpty()){
                address = addresses[0]

                for(i in 0 .. address.maxAddressLineIndex){
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        }catch (e : IOException){
            Log.e("MapsActivity",e.localizedMessage)
        }

        return addressText

    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
    }

    private fun placeMarker(latLong: LatLng) {

        val markerOption = MarkerOptions().position(latLong)

        val title = getAddress(latLong)

        markerOption.title(title)

        markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources,R.mipmap.mymarker)))

        mMap.addMarker(markerOption)


    }
}
