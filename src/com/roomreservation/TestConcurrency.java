package com.roomreservation;

import com.google.protobuf.InvalidProtocolBufferException;
import com.roomreservation.common.CentralRepositoryUtils;
import com.roomreservation.common.Parsing;
import com.roomreservation.protobuf.protos.CentralRepository;
import com.roomreservation.protobuf.protos.ResponseObject;
import com.roomreservation.service.RoomReservation;
import com.roomreservation.service.RoomReservationImplService;

import java.net.MalformedURLException;
import java.net.URL;

public class TestConcurrency {
    private static final int NUM_BOOKINGS = 3;
    private static RoomReservation roomReservation;

    public static void main(String[] args) throws MalformedURLException, InvalidProtocolBufferException {
        CentralRepository centralRepository = CentralRepositoryUtils.lookupServer("dvl", "web");
        if (centralRepository == null || !centralRepository.getStatus()){
            System.out.println("Unable to lookup server with central repository");
            System.exit(1);
        }
        RoomReservationImplService service = new RoomReservationImplService(new URL("http", centralRepository.getHost(), centralRepository.getPort(), centralRepository.getPath()));
        roomReservation = service.getRoomReservationImplPort();

        // Setup
        String[] bookingIds = setup();
        for (String bookingId: bookingIds){
            System.out.println("SETUP: Booking (" + bookingId + ") created");
        }

        // Get prepared threads
        Thread[] threadGroup1 = changeReservation(bookingIds);
        Thread[] threadGroup2 = bookRoom();

        // Start all threads at once
        for (Thread thread: threadGroup1)
            thread.start();
        for (Thread thread: threadGroup2)
            thread.start();
    }

    private static String[] setup() throws InvalidProtocolBufferException {
        String[] bookingIds = new String[NUM_BOOKINGS];
        for (int roomNum=201; roomNum<201+(NUM_BOOKINGS); roomNum++){
            ResponseObject responseObject = ResponseObject.parseFrom(roomReservation.bookRoom("dvls1234", Parsing.tryParseCampus("dvl").toString(), roomNum, "2021-01-01", "9:30-10:00"));
            bookingIds[roomNum-201] = responseObject.getMessage().split(":")[3].trim() + ":" + responseObject.getMessage().split(":")[4];
        }
        return bookingIds;
    }

    private static Thread[] changeReservation(String[] bookingIds){
        int roomNumber = 201;
        int counter = 0;
        Thread[] threads = new Thread[NUM_BOOKINGS];
        for (String bookingId: bookingIds){
            int finalRoomNumber = roomNumber;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ResponseObject responseObject = ResponseObject.parseFrom(roomReservation.changeReservation("dvls1234", bookingId, Parsing.tryParseCampus("dvl").toString(), finalRoomNumber, "2021-01-02", "9:30-10:00"));
                        System.out.println("Student (dvls1234): changeReservation (" + responseObject.getStatus() + ") in room (" + finalRoomNumber + ")");
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                }
            });
            threads[counter++] = thread;
            roomNumber++;
        }
        return threads;
    }

    private static Thread[] bookRoom(){
        int counter = 0;
        Thread[] threads = new Thread[NUM_BOOKINGS];
        for (int roomNumber = 201; roomNumber<(201+NUM_BOOKINGS); roomNumber++){
            int finalRoomNumber = roomNumber;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ResponseObject responseObject = ResponseObject.parseFrom(roomReservation.bookRoom("dvls1235", Parsing.tryParseCampus("dvl").toString(), (short) finalRoomNumber, "2021-01-02", "9:30-10:00"));
                        System.out.println("Student (dvls1235): bookRoom ("  + responseObject.getStatus() + ") in room (" + finalRoomNumber + ")");
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                }
            });
            threads[counter++] = thread;
        }
        return threads;
    }
}
