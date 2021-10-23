package com.roomreservation;

import com.roomreservation.common.Campus;
import com.roomreservation.common.CentralRepositoryUtils;
import com.roomreservation.protobuf.protos.*;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.roomreservation.common.ConsoleColours.*;

public class Server {

    private static RoomReservationImpl roomReservationImpl;

    public static void main(String[] args) {
        try {
            if (args.length <= 1) {
                Campus campus = getCampus(args[0]);
                startWebServices(campus);
                startUDPServer(campus); // For internal communication between servers
            } else {
                System.err.println("Please only specify one parameter");
                System.exit(1);
            }
        }
        catch (Exception e){
            System.err.println("Usage: java Server [CAMPUS]" + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Starts UDP server to start accepting UDP requests
     * @param campus Campus name (dvl, wst, kkl)
     */
    private static void startUDPServer(Campus campus){
        DatagramSocket datagramSocket = null;
        try {
            // Lookup server to see if it is already registered
            int remotePort;
            CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus.toString(), "udp");
            if (centralRepository != null && centralRepository.getStatus()) {
                remotePort = centralRepository.getPort();
            } else {
                remotePort = CentralRepositoryUtils.getServerPort();
                if (remotePort == -1){
                    System.out.println(ANSI_RED + "Unable to get available port, central repository may be down" + RESET);
                    System.exit(1);
                }
                if (!CentralRepositoryUtils.registerServer(campus.toString(), "udp", remotePort, CentralRepositoryUtils.SERVER_PATH, CentralRepositoryUtils.SERVER_HOST)){
                    System.out.println(ANSI_RED + "Unable to register server, central repository may be down" + RESET);
                    System.exit(1);
                }
            }
            datagramSocket = new DatagramSocket(remotePort);
            System.out.println("UDP Server ready (port: " + remotePort + ")");
            byte[] buffer = new byte[1000];

            while (true){
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);

                // Launch a new thread for each request
                DatagramSocket finalDatagramSocket = datagramSocket;
                Thread thread = new Thread(() -> {
                    try {
                        handleUDPRequest(finalDatagramSocket, datagramPacket);
                    } catch (IOException | ParseException e) {
                        System.out.println(ANSI_RED + "Exception: " + e.getMessage() + RESET);
                    }
                });
                thread.start();
            }
        }
        catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
            System.exit(1);
        }
        catch (IOException e){
            System.out.println("IO Exception: " + e.getMessage());
            System.exit(1);
        }
        catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            System.exit(1);
        }
        finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
    }

    private static void startWebServices(Campus campus) throws IOException {
        // Lookup server to see if it is already registered
        int remotePort;
        String host = "localhost";
        String path = "/roomreservation";
        CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus.toString(), "web");
        if (centralRepository != null && centralRepository.getStatus()) {
            remotePort = centralRepository.getPort();
        } else {
            remotePort = CentralRepositoryUtils.getServerPort();
            if (remotePort == -1){
                System.out.println(ANSI_RED + "Unable to get available port, central repository may be down" + RESET);
                System.exit(1);
            }
            if (!CentralRepositoryUtils.registerServer(campus.toString(), "web", remotePort, path, host)){
                System.out.println(ANSI_RED + "Unable to register server, central repository may be down" + RESET);
                System.exit(1);
            }
        }
        roomReservationImpl = new RoomReservationImpl(campus);
        Endpoint endpoint = Endpoint.create(roomReservationImpl);
        endpoint.publish("http://" + host + ":" + remotePort + path);
        System.out.println("Web Server ready (port: " + remotePort + ")");
    }

    /**
     * Thread method to handle incoming UDP request
     * @param datagramSocket Datagram Socket
     * @param datagramPacket Datagram Packet
     * @throws IOException Exception
     * @throws ParseException Exception
     */
    private static void handleUDPRequest(DatagramSocket datagramSocket, DatagramPacket datagramPacket) throws IOException, ParseException {
        // Decode request object
        RequestObject requestObject = RequestObject.parseFrom(CentralRepositoryUtils.trim(datagramPacket));

        // Build response object
        byte[] response;
        ResponseObject.Builder tempObject;

        // Perform action
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        switch (RequestObjectAction.valueOf(requestObject.getAction())){
            case GetAvailableTimeslots:
                response = roomReservationImpl.getAvailableTimeSlotOnCampus(requestObject.getDate());
                break;
            case BookRoom:
                response = roomReservationImpl.bookRoom(requestObject.getIdentifier(), requestObject.getCampusName(), requestObject.getRoomNumber(), requestObject.getDate(), requestObject.getTimeslot());
                break;
            case CancelBooking:
                response = roomReservationImpl.cancelBooking(requestObject.getIdentifier(), requestObject.getBookingId());
                break;
            case GetBookingCount:
                response = roomReservationImpl.getBookingCount(requestObject.getIdentifier(), dateFormat.parse(requestObject.getDate()));
                break;
            case CreateRoom:
                tempObject = ResponseObject.newBuilder();
                tempObject.setMessage("Create Room not supported through UDP");
                tempObject.setDateTime(dateFormat.format(new Date()));
                tempObject.setRequestType(RequestObjectAction.CreateRoom.toString());
                tempObject.setRequestParameters("None");
                tempObject.setStatus(false);
                response = tempObject.build().toByteArray();
                break;
            case DeleteRoom:
            default:
                tempObject = ResponseObject.newBuilder();
                tempObject.setMessage("Delete Room not supported through UDP");
                tempObject.setDateTime(dateFormat.format(new Date()));
                tempObject.setRequestType(RequestObjectAction.DeleteRoom.toString());
                tempObject.setRequestParameters("None");
                tempObject.setStatus(false);
                response = tempObject.build().toByteArray();
                break;
        }
        // Encode response object
        DatagramPacket reply = new DatagramPacket(response, response.length, datagramPacket.getAddress(), datagramPacket.getPort());
        datagramSocket.send(reply);
    }

    /**
     * Parses campus name
     * @param campus Campus name (dvl, wst, kkl)
     * @return Campus enum
     */
    private static Campus getCampus(String campus) {
        Pattern pattern = Pattern.compile("(dvl|kkl|wst)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(campus);
        if (!matcher.find()) {
            System.out.print(ANSI_RED + "Invalid campus! Campus must be (DVL/KKL/WST)");
            System.exit(1);
        }
        switch (campus){
            case "dvl":
                return Campus.DVL;
            case "kkl":
                return Campus.KKL;
            case "wst":
            default:
                return Campus.WST;
        }
    }
}
