import net.sf.ntru.encrypt.EncryptionPublicKey;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ChatConnection
{
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Boolean isLoggedIn;
    private NTRUContext ntru_ctx;

    public ChatConnection(String address, int port, Alias self) throws IOException
    {
        this.ntru_ctx = new NTRUContext(self);
        socket = new Socket(address, port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
        // Handshake With server
        //ntru_ctx.getKp_pub().writeTo(outputStream);

        outputStream.writeInt(ntru_ctx.getKp_pub().getEncoded().length);
        outputStream.write(ntru_ctx.getKp_pub().getEncoded());

        this.ntru_ctx.setPeer_kp(new EncryptionPublicKey(inputStream.readNBytes(inputStream.readInt())));
        byte[] hs = inputStream.readNBytes(inputStream.readInt());
        byte[] solution = this.ntru_ctx.encrypt(this.ntru_ctx.decrypt(hs));

        outputStream.writeInt(solution.length);
        outputStream.write(solution);

        if (inputStream.readUTF().equals("\\acc"))
        {
            new Thread(new ClientChatListener()).start();
            isLoggedIn = true;

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (isLoggedIn)
            {
                if (System.in.available() > 0)
                {
                    outputStream.flush();
                    outputStream.writeUTF(reader.readLine());
                }
            }
        }
        else
        {
            System.out.println(String.format("Connection rejected by %s", socket.getInetAddress().toString()));
        }
        outputStream.close();
        inputStream.close();
        socket.close();
    }

    private class ClientChatListener implements Runnable    //Listens for messages sent from chat server, then prints
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    System.out.println(inputStream.readUTF());
                }
                catch (EOFException e)
                {
                    isLoggedIn = false;
                    break;
                }
                catch (SocketException e)
                {
                    System.out.println("Session was ended remotely");
                    break;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}