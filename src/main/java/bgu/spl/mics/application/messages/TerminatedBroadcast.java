package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TerminatedBroadcast implements Broadcast{
    String sender;

   public TerminatedBroadcast(String sender) {
    this.sender = sender;
   }

   public String getSender(){
    return sender;
   }

}
