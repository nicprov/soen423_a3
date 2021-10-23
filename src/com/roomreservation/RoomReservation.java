package com.roomreservation;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.util.ArrayList;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface RoomReservation {
    /* Admin role */
    @WebMethod
    byte[] createRoom(int roomNumber, String date, ArrayList<String> listOfTimeSlots);
    @WebMethod
    byte[] deleteRoom(int roomNumber, String date, ArrayList<String> listOfTimeSlots);

    /* Student role */
    @WebMethod
    byte[] bookRoom(String identifier, String campusName, int roomNumber, String date, String timeslot);
    @WebMethod
    byte[] getAvailableTimeSlot(String date);
    @WebMethod
    byte[] cancelBooking(String identifier, String bookingId);
    @WebMethod
    byte[] changeReservation(String identifier, String bookingId, String newCampusName, int newRoomNumber, String newDate, String newTimeslot);
}