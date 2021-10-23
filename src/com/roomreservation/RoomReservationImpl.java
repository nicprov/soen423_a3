package com.roomreservation;

import com.google.protobuf.InvalidProtocolBufferException;
import com.roomreservation.collection.Entry;
import com.roomreservation.collection.LinkedPositionalList;
import com.roomreservation.collection.Node;
import com.roomreservation.collection.Position;
import com.roomreservation.common.*;
import com.roomreservation.protobuf.protos.CentralRepository;
import com.roomreservation.protobuf.protos.RequestObject;
import com.roomreservation.protobuf.protos.ResponseObject;
import com.roomreservation.protobuf.protos.RequestObjectAction;

import javax.jws.WebService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static com.roomreservation.common.ConsoleColours.ANSI_RED;
import static com.roomreservation.common.ConsoleColours.RESET;

@WebService(endpointInterface = "com.roomreservation.RoomReservation")
public class RoomReservationImpl implements RoomReservation {

    private final LinkedPositionalList<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> database;
    private final LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingCount;
    private final String logFilePath;
    private final Campus campus;
    public final DateFormat dateFormat;

    public RoomReservationImpl() throws IOException {
        this.database = new LinkedPositionalList<>();
        this.bookingCount = new LinkedPositionalList<>();
        this.campus = Campus.DVL;
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        logFilePath = "log/server/" + this.campus.toString() + ".csv";
        Logger.initializeLog(logFilePath);
        //this.generateSampleData();
    }

    protected RoomReservationImpl(Campus campus) throws IOException {
        this.database = new LinkedPositionalList<>();
        this.bookingCount = new LinkedPositionalList<>();
        this.campus = campus;
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        logFilePath = "log/server/" + this.campus.toString() + ".csv";
        Logger.initializeLog(logFilePath);
        //this.generateSampleData();
    }

    /**
     * Create Room RMI method
     * @param roomNumber Campus room number
     * @param date Date
     * @param listOfTimeSlots List of timeslots to add
     * @return RMI response object
     * @throws IOException Exception
     */
    @Override
    public synchronized byte[] createRoom(int roomNumber, String date, ArrayList<String> listOfTimeSlots) {
        Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition = findDate(date);
        boolean timeSlotCreated = false;
        boolean roomExist = false;
        if (datePosition == null){
            // Date not found so create date entry
            LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslots = new LinkedPositionalList<>();
            for (String timeslot: listOfTimeSlots){
                timeslots.addFirst(new Node<>(timeslot, null));
            }
            //database.addFirst(new Node<>(date, new LinkedPositionalList<>(new Node<>(roomNumber, timeslots))));
        } else {
            // Date exist, check if room exist
            Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition = findRoom(roomNumber, datePosition);
            if (roomPosition == null) {
                // Room not found so create room
                LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslots = new LinkedPositionalList<>();
                for (String timeslot: listOfTimeSlots){
                    timeslots.addFirst(new Node<>(timeslot, null));
                }
                datePosition.getElement().getValue().addFirst(new Node<>(roomNumber, timeslots));
            } else {
                // Room exist, so check if timeslot exist
                roomExist = true;
                for (String timeslot: listOfTimeSlots){
                    if (findTimeslot(timeslot, roomPosition) == null) {
                        // Timeslot does not exist, so create it, skip otherwise
                        roomPosition.getElement().getValue().addFirst(new Node<>(timeslot, null));
                        timeSlotCreated = true;
                    }
                }
            }
        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        if (!roomExist) {
            responseObject.setMessage("Created room (" + roomNumber + ")");
            responseObject.setStatus(true);
        } else if (!timeSlotCreated){
            responseObject.setMessage("Room already exist with specified timeslots");
            responseObject.setStatus(false);
        } else {
            responseObject.setMessage("Added timeslots to room (" + roomNumber + ")");
            responseObject.setStatus(true);
        }
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.CreateRoom.toString());
        responseObject.setRequestParameters("Room number: " + roomNumber + " | Date: " + dateFormat.format(date) + " | List of Timeslots: " + listOfTimeSlots);
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Delete room RMI method
     * @param roomNumber Campus room number
     * @param date Date
     * @param listOfTimeSlots List of time slots to remove
     * @return RMI Response object
     * @throws IOException Exception
     */
    @Override
    public synchronized byte[] deleteRoom(int roomNumber, String date, ArrayList<String> listOfTimeSlots) {
        Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition = findDate(date);
        boolean timeslotExist = false;
        if (datePosition != null){
            // Date exist, check if room exist
            Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition = findRoom(roomNumber, datePosition);
            if (roomPosition != null) {
                // Room found, search for timeslots
                for (String timeslot: listOfTimeSlots){
                    Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotPosition = findTimeslot(timeslot, roomPosition);
                    if (timeslotPosition != null) {
                        if (timeslotPosition.getElement().getValue() != null) {
                            for (Position<Entry<String, String>> timeslotPropertiesNext : timeslotPosition.getElement().getValue().positions()) {
                                if (timeslotPropertiesNext.getElement().getValue().equals("studentId")) {
                                    // Reduce booking count for student
                                    decreaseBookingCounter(timeslotPropertiesNext.getElement().getValue(), datePosition.getElement().getKey());
                                }
                            }
                        }

                        // Timeslot exists, so delete it
                        roomPosition.getElement().getValue().remove(timeslotPosition);
                        timeslotExist = true;
                    }
                }
            }
        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        if (!timeslotExist){
            responseObject.setMessage("No timeslots to delete on (" + dateFormat.format(date) + ")");
            responseObject.setStatus(false);
        } else {
            responseObject.setMessage("Removed timeslots from room (" + roomNumber + ")");
            responseObject.setStatus(true);
        }
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.CreateRoom.toString());
        responseObject.setRequestParameters("Room number: " + roomNumber + " | Date: " + dateFormat.format(date) + " | List of Timeslots: " + listOfTimeSlots);
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Book Room RMI method
     * @param identifier User ID (ie. dvls1234)
     * @param campus Campus name (dvl, wst, kkl)
     * @param roomNumber Campus room number
     * @param date Date
     * @param timeslot Timeslot to book
     * @return RMI response object
     * @throws IOException Exception
     */
    @Override
    public synchronized byte[] bookRoom(String identifier, String campus, int roomNumber, String date, String timeslot) {
        if (Campus.valueOf(campus).equals(this.campus))
            return bookRoomOnCampus(identifier, roomNumber, date, timeslot);
        else {
            // Perform action on remote server
            RequestObject.Builder requestObject = RequestObject.newBuilder();
            requestObject.setAction(RequestObjectAction.BookRoom.toString());
            requestObject.setIdentifier(identifier);
            requestObject.setRoomNumber(roomNumber);
            requestObject.setCampusName(campus.toString());
            requestObject.setDate(dateFormat.format(date));
            requestObject.setTimeslot(timeslot);
            return udpTransfer(Campus.valueOf(campus), requestObject.build());
        }
    }

    /**
     * Get available timeslot RMI method
     * @param date Date
     * @return RMI response object
     * @throws IOException Exception
     */
    @Override
    public synchronized byte[] getAvailableTimeSlot(String date) {
        // Build new proto request object
        RequestObject.Builder requestObject = RequestObject.newBuilder();
        requestObject.setAction(RequestObjectAction.GetAvailableTimeslots.toString());
        requestObject.setDate(dateFormat.format(date));

        ResponseObject dvlTimeslots = null;
        ResponseObject kklTimeslots = null;
        ResponseObject wstTimeslots = null;
        try {
            // Get response object from each campus
            dvlTimeslots = ResponseObject.parseFrom(udpTransfer(Campus.DVL, requestObject.build()));
            kklTimeslots = ResponseObject.parseFrom(udpTransfer(Campus.KKL, requestObject.build()));
            wstTimeslots = ResponseObject.parseFrom(udpTransfer(Campus.WST, requestObject.build()));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        String message = "";
        if (dvlTimeslots.getStatus())
            message += "DVL " + dvlTimeslots.getMessage() + " ";
        else
            message += "DVL (no response from server) ";
        if (kklTimeslots.getStatus())
            message += "KKL " + kklTimeslots.getMessage() + " ";
        else
            message += "KKL (no response from server) ";
        if (wstTimeslots.getStatus())
            message += "WST " + wstTimeslots.getMessage();
        else
            message += "WST (no response from server)";

        //  Create response object for rmi
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        responseObject.setMessage(message);
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.GetAvailableTimeslots.toString());
        responseObject.setRequestParameters("Date: " + dateFormat.format(date));
        responseObject.setStatus(true);
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Cancel booking RMI method
     * @param identifier User ID (ie. dvls1234)
     * @param bookingId Booking id
     * @return RMI response object
     * @throws IOException Exception
     */
    @Override
    public synchronized byte[] cancelBooking(String identifier, String bookingId) {
        Campus campus = Campus.valueOf(bookingId.split(":")[0]);
        if (campus.equals(this.campus))
            return cancelBookingOnCampus(identifier, bookingId);
        else {
            // Perform action on remote server
            RequestObject.Builder requestObject = RequestObject.newBuilder();
            requestObject.setAction(RequestObjectAction.CancelBooking.toString());
            requestObject.setIdentifier(identifier);
            requestObject.setBookingId(bookingId);
            return udpTransfer(campus, requestObject.build());
        }
    }

    @Override
    public byte[] changeReservation(String identifier, String bookingId, String newCampusName, int newRoomNumber, String newDate, String newTimeslot) {
        return new byte[0];
    }

    /**
     * Counts the number of available timeslots on a given day in the given campus
     * @param date Date
     * @return RMI response object
     * @throws IOException Exception
     */
    public byte[] getAvailableTimeSlotOnCampus(Date date) {
        int counter = 0;
        for (Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> dateNext : database.positions()) {
            for (Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomNext : dateNext.getElement().getValue().positions()) {
                for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotNext : roomNext.getElement().getValue().positions()) {
                    if (timeslotNext.getElement().getValue() == null && dateNext.getElement().getKey().equals(date))
                        counter++;
                }
            }
        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        responseObject.setMessage(Integer.toString(counter));
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.GetAvailableTimeslots.toString());
        responseObject.setRequestParameters("Date: " + dateFormat.format(date));
        responseObject.setStatus(true);
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Counts the number of bookings on a specific date for a specific user
     * @param identifier User ID (ie. dvls1234)
     * @param date Date
     * @return RMI response object
     */
    public byte[] getBookingCount(String identifier, String date) {
//        int counter = 0;
//        LocalDate tempDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//        for (Position<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingIdentifier: bookingCount.positions()){
//            if (bookingIdentifier.getElement().getKey().equals(identifier)) {
//                for (Position<Entry<Date, Integer>> bookingDate: bookingIdentifier.getElement().getValue().positions()){
//                    // Counter date is >= than provided date (-1 week) and Counter date is < provided date
//                    if ((bookingDate.getElement().getKey().compareTo(Date.from(tempDate.minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant())) > 0)
//                        && (bookingDate.getElement().getKey().compareTo(Date.from(tempDate.atStartOfDay(ZoneId.systemDefault()).toInstant())) <= 0)){
//                        // Within 1 week so it counts
//                        counter += bookingDate.getElement().getValue();
//                    }
//                }
//            }
//        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        responseObject.setStatus(true);
        responseObject.setMessage(Integer.toString(1));
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.CreateRoom.toString());
        responseObject.setRequestParameters("Identifier: " + identifier + " | Date: " + dateFormat.format(date));
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Books room for a specific user in a specific room, on a specific day and timeslot
     * @param identifier User ID (ie. dvls1234)
     * @param roomNumber Room number
     * @param date Date
     * @param timeslot Timeslot
     * @return RMI response object
     * @throws IOException Exception
     */
    private byte[] bookRoomOnCampus(String identifier, int roomNumber, String date, String timeslot) {
        boolean isOverBookingCountLimit = false;
        boolean timeslotExist = false;
        boolean isBooked = false;
        String bookingId = "";
        Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition = findDate(date);
        if (datePosition != null) {
            // Date exist, check if room exist
            Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition = findRoom(roomNumber, datePosition);
            if (roomPosition != null) {
                // Room found, search for timeslots
                Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotPosition = findTimeslot(timeslot, roomPosition);

                // Check if timeslot exist
                if (timeslotPosition != null) {
                    timeslotExist = true;

                    // Check booking count for this week on all campuses
                    RequestObject.Builder requestBookingCount = RequestObject.newBuilder();
                    requestBookingCount.setIdentifier(identifier);
                    requestBookingCount.setDate(dateFormat.format(date));
                    requestBookingCount.setAction(RequestObjectAction.GetBookingCount.toString());
                    ResponseObject dvlBookingCount = null;
                    ResponseObject kklBookingCount = null;
                    ResponseObject wstBookingCount = null;
                    try {
                        dvlBookingCount = ResponseObject.parseFrom(udpTransfer(Campus.DVL, requestBookingCount.build()));
                        kklBookingCount = ResponseObject.parseFrom(udpTransfer(Campus.KKL, requestBookingCount.build()));
                        wstBookingCount = ResponseObject.parseFrom(udpTransfer(Campus.WST, requestBookingCount.build()));
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }

                    int totalBookingCount = 0;
                    if (dvlBookingCount.getStatus())
                        totalBookingCount += Integer.parseInt(dvlBookingCount.getMessage());
                    if (kklBookingCount.getStatus())
                        totalBookingCount += Integer.parseInt(kklBookingCount.getMessage());
                    if (wstBookingCount.getStatus())
                        totalBookingCount += Integer.parseInt(wstBookingCount.getMessage());

                    // Increase if total booking count < 3, increase
                    if (totalBookingCount < 3) {
                        //Increase booking count
                        increaseBookingCounter(identifier, date);

                        if (timeslotPosition.getElement().getValue() == null){
                            // Create timeslot and add attributes
                            isBooked = true;
                            bookingId = this.campus + ":" + UUID.randomUUID();
                            roomPosition.getElement().getValue().set(timeslotPosition, new Node<>(timeslot, new LinkedPositionalList<>()));
                            timeslotPosition.getElement().getValue().addFirst(new Node<>("bookingId", bookingId));
                            timeslotPosition.getElement().getValue().addFirst(new Node<>("studentId", identifier));
                        }
                    } else
                        isOverBookingCountLimit = true;
                }
            }
        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        if (!timeslotExist){
            responseObject.setMessage("Timeslot (" + timeslot + ") does not exist on (" + dateFormat.format(date) + ")");
            responseObject.setStatus(false);
        } else if (isOverBookingCountLimit) {
            responseObject.setMessage("Unable to book room, maximum booking limit is reached");
            responseObject.setStatus(false);
        } else if (isBooked){
            responseObject.setMessage("Timeslot (" + timeslot + ") has been booked | Booking ID: " + bookingId);
            responseObject.setStatus(true);
        } else {
            responseObject.setMessage("Unable to book room, timeslot (" + timeslot + ") has already booked");
            responseObject.setStatus(false);
        }
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.CreateRoom.toString());
        responseObject.setRequestParameters("Identifier: " + identifier + " | Room Number: " + roomNumber + " | Date: " + dateFormat.format(date) + " | Timeslot: " + timeslot);
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Cancels booking on campus for a specific user and booking id
     * @param identifier User
     * @param bookingId Booking id
     * @return RMI response object
     * @throws IOException Exception
     */
    private byte[] cancelBookingOnCampus(String identifier, String bookingId) {
        boolean bookingExist = false;
        boolean studentIdMatched = false;
        for (Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition : database.positions()) {
            for (Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition : datePosition.getElement().getValue().positions()) {
                for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotPosition : roomPosition.getElement().getValue().positions()) {
                    if (timeslotPosition.getElement().getValue() != null){
                        for (Position<Entry<String, String>> timeslotPropertiesPosition : timeslotPosition.getElement().getValue().positions()) {
                            if (timeslotPropertiesPosition.getElement().getKey().equals("studentId") && timeslotPropertiesPosition.getElement().getValue().equals(identifier)){
                                studentIdMatched = true;
                            }
                            if (timeslotPropertiesPosition.getElement().getKey().equals("bookingId") && timeslotPropertiesPosition.getElement().getValue().equals(bookingId)){
                                bookingExist = true;
                            }
                        }
                        if (bookingExist && studentIdMatched){
                            // Reduce booking count
                            decreaseBookingCounter(identifier, datePosition.getElement().getKey());

                            // Cancel booking
                            roomPosition.getElement().getValue().set(timeslotPosition, new Node<>(timeslotPosition.getElement().getKey(), null));
                        }
                    }
                }
            }
        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        if (!bookingExist){
            responseObject.setMessage("Booking (" + bookingId + ") does not exist");
            responseObject.setStatus(false);
        } else if (!studentIdMatched) {
            responseObject.setMessage("Booking (" + bookingId + ") is reserved to another student");
            responseObject.setStatus(false);
        } else {
            responseObject.setMessage("Cancelled booking (" + bookingId + ")");
            responseObject.setStatus(true);
        }
        responseObject.setDateTime(new Date().toString());
        responseObject.setRequestType(RequestObjectAction.CreateRoom.toString());
        responseObject.setRequestParameters("Booking Id: " + bookingId);
        //Logger.log(logFilePath, rmiResponse);
        return responseObject.build().toByteArray();
    }

    /**
     * Increase booking count for specific user on specific date
     * @param identifier User ID (ie. dvls1234)
     * @param date Date
     */
    public void increaseBookingCounter(String identifier, String date) {
        boolean foundIdentifier = false;
        boolean foundDate = false;
        for (Position<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingIdentifier: bookingCount.positions()){
            if (bookingIdentifier.getElement().getKey().equals(identifier)) {
                foundIdentifier = true;
                for (Position<Entry<Date, Integer>> bookingDate: bookingIdentifier.getElement().getValue().positions()){
                    if (bookingDate.getElement().getKey().equals(date)){
                        foundDate = true;
                        // Increase count
                        // bookingIdentifier.getElement().getValue().set(bookingDate, new Node<>(date, bookingDate.getElement().getValue() + 1));
                    }
                }
//                if (!foundDate){
//                    bookingIdentifier.getElement().getValue().addFirst(new Node<>(date, 1));
//                }
            }
        }
        //if (!foundIdentifier)
        //    bookingCount.addFirst(new Node<>(identifier, new LinkedPositionalList<>(new Node<>(date, 1))));
    }

    /**
     * Decreases booking count for specific user on specific date
     * @param identifier User ID (ie. dvls1234)
     * @param date Date
     */
    public void decreaseBookingCounter(String identifier, Date date) {
        for (Position<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingIdentifier: bookingCount.positions()){
            if (bookingIdentifier.getElement().getKey().equals(identifier)) {
                for (Position<Entry<Date, Integer>> bookingDate: bookingIdentifier.getElement().getValue().positions()){
                    if (bookingDate.getElement().getKey().equals(date)){
                        // Decrease count
                        bookingIdentifier.getElement().getValue().set(bookingDate, new Node<>(date, bookingDate.getElement().getValue() - 1));
                    }
                }
            }
        }
    }

    /**
     * Performs a UDP request on a specific campus by first performing a looking with the central repository
     * @param campus Campus name (dvl, wst, kkl)
     * @param requestObject Request Object
     * @return RMI response object
     */
    private byte[] udpTransfer(Campus campus, RequestObject requestObject){
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(1000); // Set timeout
            InetAddress host = InetAddress.getLocalHost();

            CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus.toString(), "udp");
            if (centralRepository != null && centralRepository.getStatus()){
                DatagramPacket request = new DatagramPacket(requestObject.toByteArray(), requestObject.toByteArray().length, host, centralRepository.getPort());
                datagramSocket.send(request);
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(reply);
                return trim(reply);
            } else {
                System.out.println(ANSI_RED + "Unable to get server details from the central repository" + RESET);
                ResponseObject.Builder responseObject = ResponseObject.newBuilder();
                responseObject.setStatus(false);
                responseObject.setMessage("Unable to get server details from the central repository");
                return responseObject.build().toByteArray();
            }
        }
        catch (SocketException e){
            System.out.println(ANSI_RED + "Socket: " + e.getMessage() + RESET);
        } catch (IOException e){
            System.out.println(ANSI_RED + "IO: " + e.getMessage() + RESET);
        } finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        responseObject.setStatus(false);
        responseObject.setMessage("Unable to connect to remote server");
        return responseObject.build().toByteArray();
    }

    /**
     * Trims byte array to strip 0s filling up unused elements
     * @param packet Datagram packet
     * @return Trimmed byte array
     */
    private static byte[] trim(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    /**
     * Searches database to find position at specific date
     * @param date Date
     * @return Date position in database
     */
    private Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> findDate(String date){
        for (Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> dateNext : database.positions()) {
            if (dateNext.getElement().getKey().equals(date))
                return dateNext;
        }
        return null;
    }

    /**
     * Searches database to find position at specific room number
     * @param roomNumber Campus room number
     * @param datePosition Date position object
     * @return Room position in database
     */
    private Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> findRoom(int roomNumber, Position<Entry<Date, LinkedPositionalList<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition){
        for (Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomNext : datePosition.getElement().getValue().positions()) {
            if (roomNext.getElement().getKey().equals(roomNumber))
                return roomNext;
        }
        return null;
    }

    /**
     * Searches database to find position at specific timeslot
     * @param timeslot Timeslot
     * @param room Room position object
     * @return Timeslot position in database
     */
    private Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> findTimeslot(String timeslot, Position<Entry<Integer, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> room){
        for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotNext : room.getElement().getValue().positions()) {
            if (timeslotNext.getElement().getKey().equals(timeslot))
                return timeslotNext;
        }
        return null;
    }

    /**
     * Generates sample data in campus
     */
    private void generateSampleData(){
        this.createRoom(201, Parsing.tryParseDate("2021-01-01"), Parsing.tryParseTimeslotList("9:30-10:00"));
        this.createRoom(202, Parsing.tryParseDate("2021-01-02"), Parsing.tryParseTimeslotList("10:30-11:00"));
        this.createRoom(203, Parsing.tryParseDate("2021-01-03"), Parsing.tryParseTimeslotList("11:00-11:30"));
        this.createRoom(204, Parsing.tryParseDate("2021-01-04"), Parsing.tryParseTimeslotList("11:30-12:00"));
        this.createRoom(205, Parsing.tryParseDate("2021-01-05"), Parsing.tryParseTimeslotList("12:00-12:30"));
    }
}
