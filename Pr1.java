import java.util.concurrent.*;
import java.util.*;

class Mail {
    String sender;
    String receiver;

    public Mail(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }
}

class PostOffice {
    private final BlockingQueue<Mail> clientQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Mail> acceptedMails = new LinkedBlockingQueue<>();
    private volatile boolean isOpen = true;

    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        isOpen = false;
        System.out.println("🔒 Пошта закрита");
    }

    public void addClient(Mail mail) {
        if (isOpen) {
            System.out.println("👤 Прийшов замовник: " + mail.sender);
            clientQueue.offer(mail);
        } else {
            System.out.println("❌ " + mail.sender + " прийшов, але пошта вже закрита");
        }
    }

    public Mail getNextClient() {
        return clientQueue.poll();
    }

    public void acceptMail(Mail mail) {
        if (isOpen) {
            System.out.println("📥 Замовлення від " + mail.sender + " прийняте");
            acceptedMails.offer(mail);
        }
    }

    public Mail getNextMailToSend() {
        return acceptedMails.poll();
    }

    public boolean hasClients() {
        return !clientQueue.isEmpty();
    }

    public boolean hasPendingMails() {
        return !acceptedMails.isEmpty();
    }
}

class Sender implements Runnable {
    private final PostOffice postOffice;
    private final String name;
    private final Random random = new Random();

    public Sender(String name, PostOffice postOffice) {
        this.name = name;
        this.postOffice = postOffice;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(random.nextInt(1000) + 500); // Швидкий потік клієнтів
                String receiver = "Receiver" + (random.nextInt(3) + 1);
                postOffice.addClient(new Mail(name, receiver));
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

class PostWorker implements Runnable {
    private final PostOffice postOffice;

    public PostWorker(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Пріоритет — обслуговування клієнтів
                while (postOffice.hasClients()) {
                    Mail mail = postOffice.getNextClient();
                    if (mail != null && postOffice.isOpen()) {
                        postOffice.acceptMail(mail);
                    }
                }

                // Якщо немає клієнтів — відправляємо посилки
                if (!postOffice.hasClients()) {
                    Mail mail = postOffice.getNextMailToSend();
                    if (mail != null) {
                        System.out.println("📤 Замовлення від " + mail.sender + " відправлено");
                        Thread.sleep(500); // симуляція часу відправки
                    } else if (!postOffice.isOpen()) {
                        break;
                    }
                }

                Thread.sleep(100); // невелика пауза
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

public class Pr1 {
    public static void main(String[] args) throws InterruptedException {
        PostOffice postOffice = new PostOffice();
        System.out.println("📬 Пошта відкрита");

        Thread workerThread = new Thread(new PostWorker(postOffice));
        workerThread.start();

        List<Thread> senders = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Thread senderThread = new Thread(new Sender("Sender" + i, postOffice));
            senderThread.start();
            senders.add(senderThread);
        }

        Thread.sleep(10000); // Пошта працює 10 секунд
        postOffice.close();

        for (Thread sender : senders) {
            sender.interrupt();
        }

        workerThread.join();
        System.out.println("🏁 Симуляція завершена");
    }
}