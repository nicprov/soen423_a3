
package com.roomreservation.service;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.3.1-SNAPSHOT
 * Generated source version: 2.2
 * 
 */
@WebService(name = "RoomReservation", targetNamespace = "http://roomreservation.com/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@XmlSeeAlso({
    ObjectFactory.class
})
public interface RoomReservation {


    /**
     * 
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(partName = "return")
    @Action(input = "http://roomreservation.com/RoomReservation/createRoomRequest", output = "http://roomreservation.com/RoomReservation/createRoomResponse")
    public byte[] createRoom(
        @WebParam(name = "arg0", partName = "arg0")
        int arg0,
        @WebParam(name = "arg1", partName = "arg1")
        String arg1,
        @WebParam(name = "arg2", partName = "arg2")
        ArrayList arg2);

    /**
     * 
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(partName = "return")
    @Action(input = "http://roomreservation.com/RoomReservation/deleteRoomRequest", output = "http://roomreservation.com/RoomReservation/deleteRoomResponse")
    public byte[] deleteRoom(
        @WebParam(name = "arg0", partName = "arg0")
        int arg0,
        @WebParam(name = "arg1", partName = "arg1")
        String arg1,
        @WebParam(name = "arg2", partName = "arg2")
        ArrayList arg2);

    /**
     * 
     * @param arg3
     * @param arg2
     * @param arg4
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(partName = "return")
    @Action(input = "http://roomreservation.com/RoomReservation/bookRoomRequest", output = "http://roomreservation.com/RoomReservation/bookRoomResponse")
    public byte[] bookRoom(
        @WebParam(name = "arg0", partName = "arg0")
        String arg0,
        @WebParam(name = "arg1", partName = "arg1")
        String arg1,
        @WebParam(name = "arg2", partName = "arg2")
        int arg2,
        @WebParam(name = "arg3", partName = "arg3")
        String arg3,
        @WebParam(name = "arg4", partName = "arg4")
        String arg4);

    /**
     * 
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(partName = "return")
    @Action(input = "http://roomreservation.com/RoomReservation/getAvailableTimeSlotRequest", output = "http://roomreservation.com/RoomReservation/getAvailableTimeSlotResponse")
    public byte[] getAvailableTimeSlot(
        @WebParam(name = "arg0", partName = "arg0")
        String arg0);

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(partName = "return")
    @Action(input = "http://roomreservation.com/RoomReservation/cancelBookingRequest", output = "http://roomreservation.com/RoomReservation/cancelBookingResponse")
    public byte[] cancelBooking(
        @WebParam(name = "arg0", partName = "arg0")
        String arg0,
        @WebParam(name = "arg1", partName = "arg1")
        String arg1);

    /**
     * 
     * @param arg3
     * @param arg2
     * @param arg5
     * @param arg4
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(partName = "return")
    @Action(input = "http://roomreservation.com/RoomReservation/changeReservationRequest", output = "http://roomreservation.com/RoomReservation/changeReservationResponse")
    public byte[] changeReservation(
        @WebParam(name = "arg0", partName = "arg0")
        String arg0,
        @WebParam(name = "arg1", partName = "arg1")
        String arg1,
        @WebParam(name = "arg2", partName = "arg2")
        String arg2,
        @WebParam(name = "arg3", partName = "arg3")
        int arg3,
        @WebParam(name = "arg4", partName = "arg4")
        String arg4,
        @WebParam(name = "arg5", partName = "arg5")
        String arg5);

}
