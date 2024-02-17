public interface IChallenge {
    public byte[] get();

    public boolean submit(byte[] response);

    public User getUser();
}
