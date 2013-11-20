package jp.co.fujixerox.libjingleiceconnection;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 11/20/13
 * Time: 1:06 PM
 * To change this template use File | Settings | File Templates.
 */

import org.json.JSONException;
import org.webrtc.*;
import org.webrtc.PeerConnection.IceServer;




import java.util.LinkedList;
import org.json.JSONObject;

public class JingleIceConnection {

    private PeerConnectionFactory factory;
    private MediaConstraints sdpMediaConstraints;
    private LinkedList<IceServer> iceServers;
    private PCObserver pcObserver;
    private SdpObserver sdpObserver;
    private PeerConnection pc;
    private LinkedList<IceCandidate> queuedRemoteCandidates;
    private SignalClient signalClient;




    public JingleIceConnection()
    {

        iceServers=new LinkedList<IceServer>();
        pcObserver=new PCObserver();

        signalClient=new SignalClient();

        queuedRemoteCandidates=new LinkedList<IceCandidate>();

    }

    public void init()
    {


        factory=new PeerConnectionFactory();
        sdpMediaConstraints=new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        iceServers.add(new IceServer("stun:stun.l.google.com:19302"));
        iceServers.add(new IceServer("turn:fake.example.com", "fakeUsername", "fakePassword"));


    }

    public void createPC()
    {
          pc=factory.createPeerConnection(iceServers,sdpMediaConstraints,pcObserver);

          sdpObserver=new SDPObserver();
    }

    public void setFactory(PeerConnectionFactory factory)
    {
        this.factory=factory;
    }

    public void setSdpMediaConstraints(MediaConstraints constraints)
    {
        this.sdpMediaConstraints=constraints;
    }

    public LinkedList<IceServer> getIceServers()
    {
        return iceServers;
    }



    public static void main(String[] args)
    {
        JingleIceConnection connection=new JingleIceConnection();


        System.out.println("now init");

       connection.init();

       System.out.println("now creating peerconnection");
       connection.createPC();

    }

    private void sendMessage(JSONObject json)
    {
        signalClient.sendMessage(json);
    }


    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }



    private class PCObserver implements PeerConnection.Observer{


        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {

            JSONObject json = new JSONObject();
            jsonPut(json, "type", "candidate");
            jsonPut(json, "label", iceCandidate.sdpMLineIndex);
            jsonPut(json, "id", iceCandidate.sdpMid);
            jsonPut(json, "candidate", iceCandidate.sdp);

            sendMessage(json);

        }

        @Override
        public void onError() {

            System.out.println("PeerConnection Error!");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

            System.out.println("Not yet implemented");
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

            System.out.println("Not yet implemented");
        }


        @Override
        public void onDataChannel(DataChannel dataChannel) {

            System.out.println("Not yet implemented");
        }




    }



    private class SDPObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {


            System.out.println("Sending "+sessionDescription.type);
            SessionDescription sdp=new SessionDescription(sessionDescription.type,sessionDescription.description);
            JSONObject json = new JSONObject();
            jsonPut(json, "type", sdp.type.canonicalForm());
            jsonPut(json, "sdp", sdp.description);


            sendMessage(json);

            pc.setLocalDescription(this,sdp);
        }

        @Override
        public void onSetSuccess() {

            if(signalClient.isInitiator())
            {
                if (pc.getRemoteDescription() != null) {
                    // We've set our local offer and received & set the remote
                    // answer, so drain candidates.
                    drainRemoteCandidates();
                }
            } else {
                if (pc.getLocalDescription() == null) {
                    // We just set the remote offer, time to create our answer.
                    System.out.println("Creating answer");
                    pc.createAnswer(SDPObserver.this, sdpMediaConstraints);
                } else {
                    // Sent our answer and set it as local description; drain
                    // candidates.
                    drainRemoteCandidates();
                }
            }
            }


        @Override
        public void onCreateFailure(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onSetFailure(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        private void drainRemoteCandidates() {
        for (IceCandidate candidate : queuedRemoteCandidates) {
            pc.addIceCandidate(candidate);
        }
        queuedRemoteCandidates = null;
    }

    }






}